package com.example.audiorecordertest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class AudioRecordActivity extends Activity {

	private TextView stateView;
	private Button btnStart, btnStop, btnPlay, btnFinish;
	private File audioFile;// 临时文件
	private RecordTask recorder;
	private PlayTask player;
	private boolean isRecording = true, isPlaying = false; // 标记
	private int sampleRateInHz = 16000; // 录制频率，单位hz.
	private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO; //CHANNEL_IN_MONO ;// 录制通道
	private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;// 录制编码格式

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_audio_recorder_test);
		stateView = (TextView) this.findViewById(R.id.view_state);
		stateView.setText("准备开始");
		btnStart = (Button) this.findViewById(R.id.btn_start);
		btnStop = (Button) this.findViewById(R.id.btn_stop);
		btnPlay = (Button) this.findViewById(R.id.btn_play);
		btnFinish = (Button) this.findViewById(R.id.btn_finish);
		btnFinish.setText("停止播放");
		btnStop.setEnabled(false);
		btnPlay.setEnabled(false);
		btnFinish.setEnabled(false);
		// 创建一个文件，用于保存录制内容
		File fpath = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/audio_record_test/");
		fpath.mkdirs();// 创建文件夹
		try {
			// 创建临时文件,格式为.pcm
			audioFile = File.createTempFile("recording", ".pcm", fpath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void onClick(View v) {
		int id = v.getId();
		switch (id) {
		case R.id.btn_start:
			// 开始录制
			// 启动录制任务
			Log.i("debug", "开始录音！");
			recorder = new RecordTask();
			recorder.execute();
			break;
		case R.id.btn_stop:
			// 停止录制
			this.isRecording = false;
			// 更新状态
			// 在录制完成时设置，在RecordTask的onPostExecute中完成
			break;
		case R.id.btn_play:
			// 开始 播放
			player = new PlayTask();
			player.execute();
			break;

		case R.id.btn_finish:
			// 完成 播放
			this.isPlaying = false;
			break;
		}
	}

	// 录制
	class RecordTask extends AsyncTask<Void, Integer, Void> {
		// 后台线程执行onPreExecute()完后立即调用，这步被用于执行较长时间的后台计算
		@Override
		protected Void doInBackground(Void... params) {
			isRecording = true;
			try {
				// 开通输出流到指定的文件
				DataOutputStream dos = new DataOutputStream(
						new BufferedOutputStream(
								new FileOutputStream(audioFile)));
				// 缓冲大小BufferSize：通过静态方法getMinBufferSize来获取
				int bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz,
						channelConfig, audioFormat);
				// 实例化AudioRecord
				// AudioSource：这里可以是MediaRecorder.AudioSource.MIC
				AudioRecord record = new AudioRecord(
						MediaRecorder.AudioSource.MIC, sampleRateInHz,
						channelConfig, audioFormat, bufferSize);
				// 定义缓冲
				short[] buffer = new short[bufferSize];
				// 开始录制
				record.startRecording();
				int r = 0; // 存储录制进度
				// 定义循环，根据isRecording的值来判断是否继续录制
				/**
				 * startRecording(); 然后一个循环，调用AudioRecord的read方法实现读取
				 * 另外使用MediaPlayer是无法播放使用AudioRecord录制的音频的，为了实现播放，我们需要
				 * 使用AudioTrack类来实现 AudioTrack类允许我们播放原始的音频数据
				 */
				while (isRecording) {
					int bufferReadResult = record
							.read(buffer, 0, buffer.length);
					// 循环将buffer中的音频数据写入到OutputStream中
					for (int i = 0; i < bufferReadResult; i++) {
						dos.writeShort(buffer[i]);
					}
					publishProgress(new Integer(r)); // 向UI线程报告当前进度(onProgressUpdate
					// 调用)
					r++;
				}
				// 录制结束
				record.stop();
				dos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return null;
		}

		// 当在上面方法中调用publishProgress时，该方法触发,该方法在UI线程中被执行
		// 来更新任务的进度
		protected void onProgressUpdate(Integer... progress) {
			Log.i("debug", "onProgressUpdate");
			stateView.setText(progress[0].toString());
		}

		//  invoked on the UI thread after the background computation finishes.
		// 当后台计算结束时，调用 UI线程。后台计算结果作为一个参数传递到这步。
		protected void onPostExecute(Void result) {
			Log.i("debug", "onPostExecute");
			btnStop.setEnabled(false);
			btnStart.setEnabled(true);
			btnPlay.setEnabled(true);
			btnFinish.setEnabled(false);
		}

		// 正在录制
		//  invoked on the UI thread before the task is executed. 
		// 在UI线程上调用任务后立即执行,通常被用于设置任务
		protected void onPreExecute() {
			 stateView.setText("正在录制");
			Log.i("debug", "onPreExecute");
			btnStart.setEnabled(false);
			btnPlay.setEnabled(false);
			btnFinish.setEnabled(false);
			btnStop.setEnabled(true);
		}

	}

	// 播放
	/**
	 * 1、StreamType:在AudioManager中有几个常量，其中一个是STREAM_MUSIC;
	 * 2、SampleRateInHz：最好和AudioRecord使用的是同一个值 
	 * 3、ChannelConfig：同上
	 * 4、AudioFormat：同上 
	 * 5、BufferSize：通过AudioTrack的静态方法getMinBufferSize来获取
	 * 6、Mode：可以是AudioTrack.MODE_STREAM和MODE_STATIC，关于这两种不同之处，可以查阅文档
	 * 二、打开一个输入流，指向刚刚录制内容保存的文件，然后开始播放，边读取边播放
	 * 
	 */
	class PlayTask extends AsyncTask<Void, Integer, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			isPlaying = true;
			// 缓冲大小BufferSize：通过AudioTrack的静态方法getMinBufferSize来获取
			int bufferSize = AudioTrack.getMinBufferSize(sampleRateInHz,
					channelConfig, audioFormat);
			short[] buffer = new short[bufferSize / 4];
			// 定义输入流，将音频写入到AudioTrack类中，实现播放
			try {
				DataInputStream dis = new DataInputStream(
						new BufferedInputStream(new FileInputStream(audioFile)));
				// 实例AudioTrack
				AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC,
						sampleRateInHz, channelConfig, audioFormat, bufferSize,
						AudioTrack.MODE_STREAM);
				// 开始播放
				track.play();
				// 由于AudioTrack播放的是流，所以，我们需要一边播放一边读取
				while (isPlaying && dis.available() > 0) {
					int i = 0;
					while (dis.available() > 0 && i < buffer.length) {
						buffer[i] = dis.readShort();
						i++;
					}
					// 然后将数据写入到AudioTrack中
					track.write(buffer, 0, buffer.length);
				}
				track.stop();
				// 播放结束
				dis.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return null;
		}

		protected void onPostExecute(Void result) {
			btnPlay.setEnabled(true);
			btnFinish.setEnabled(false);
			btnStart.setEnabled(true);
			btnStop.setEnabled(false);
		}

		protected void onPreExecute() {

			// stateView.setText("正在播放");
			btnStart.setEnabled(false);
			btnStop.setEnabled(false);
			btnPlay.setEnabled(false);
			btnFinish.setEnabled(true);
		}
	}
}