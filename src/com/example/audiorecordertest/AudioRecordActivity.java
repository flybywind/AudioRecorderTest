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
	private File audioFile;// ��ʱ�ļ�
	private RecordTask recorder;
	private PlayTask player;
	private boolean isRecording = true, isPlaying = false; // ���
	private int sampleRateInHz = 16000; // ¼��Ƶ�ʣ���λhz.
	private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO; //CHANNEL_IN_MONO ;// ¼��ͨ��
	private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;// ¼�Ʊ����ʽ

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_audio_recorder_test);
		stateView = (TextView) this.findViewById(R.id.view_state);
		stateView.setText("׼����ʼ");
		btnStart = (Button) this.findViewById(R.id.btn_start);
		btnStop = (Button) this.findViewById(R.id.btn_stop);
		btnPlay = (Button) this.findViewById(R.id.btn_play);
		btnFinish = (Button) this.findViewById(R.id.btn_finish);
		btnFinish.setText("ֹͣ����");
		btnStop.setEnabled(false);
		btnPlay.setEnabled(false);
		btnFinish.setEnabled(false);
		// ����һ���ļ������ڱ���¼������
		File fpath = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/audio_record_test/");
		fpath.mkdirs();// �����ļ���
		try {
			// ������ʱ�ļ�,��ʽΪ.pcm
			audioFile = File.createTempFile("recording", ".pcm", fpath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void onClick(View v) {
		int id = v.getId();
		switch (id) {
		case R.id.btn_start:
			// ��ʼ¼��
			// ����¼������
			Log.i("debug", "��ʼ¼����");
			recorder = new RecordTask();
			recorder.execute();
			break;
		case R.id.btn_stop:
			// ֹͣ¼��
			this.isRecording = false;
			// ����״̬
			// ��¼�����ʱ���ã���RecordTask��onPostExecute�����
			break;
		case R.id.btn_play:
			// ��ʼ ����
			player = new PlayTask();
			player.execute();
			break;

		case R.id.btn_finish:
			// ��� ����
			this.isPlaying = false;
			break;
		}
	}

	// ¼��
	class RecordTask extends AsyncTask<Void, Integer, Void> {
		// ��̨�߳�ִ��onPreExecute()����������ã��ⲽ������ִ�нϳ�ʱ��ĺ�̨����
		@Override
		protected Void doInBackground(Void... params) {
			isRecording = true;
			try {
				// ��ͨ�������ָ�����ļ�
				DataOutputStream dos = new DataOutputStream(
						new BufferedOutputStream(
								new FileOutputStream(audioFile)));
				// �����СBufferSize��ͨ����̬����getMinBufferSize����ȡ
				int bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz,
						channelConfig, audioFormat);
				// ʵ����AudioRecord
				// AudioSource�����������MediaRecorder.AudioSource.MIC
				AudioRecord record = new AudioRecord(
						MediaRecorder.AudioSource.MIC, sampleRateInHz,
						channelConfig, audioFormat, bufferSize);
				// ���建��
				short[] buffer = new short[bufferSize];
				// ��ʼ¼��
				record.startRecording();
				int r = 0; // �洢¼�ƽ���
				// ����ѭ��������isRecording��ֵ���ж��Ƿ����¼��
				/**
				 * startRecording(); Ȼ��һ��ѭ��������AudioRecord��read����ʵ�ֶ�ȡ
				 * ����ʹ��MediaPlayer���޷�����ʹ��AudioRecord¼�Ƶ���Ƶ�ģ�Ϊ��ʵ�ֲ��ţ�������Ҫ
				 * ʹ��AudioTrack����ʵ�� AudioTrack���������ǲ���ԭʼ����Ƶ����
				 */
				while (isRecording) {
					int bufferReadResult = record
							.read(buffer, 0, buffer.length);
					// ѭ����buffer�е���Ƶ����д�뵽OutputStream��
					for (int i = 0; i < bufferReadResult; i++) {
						dos.writeShort(buffer[i]);
					}
					publishProgress(new Integer(r)); // ��UI�̱߳��浱ǰ����(onProgressUpdate
					// ����)
					r++;
				}
				// ¼�ƽ���
				record.stop();
				dos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return null;
		}

		// �������淽���е���publishProgressʱ���÷�������,�÷�����UI�߳��б�ִ��
		// ����������Ľ���
		protected void onProgressUpdate(Integer... progress) {
			Log.i("debug", "onProgressUpdate");
			stateView.setText(progress[0].toString());
		}

		//  invoked on the UI thread after the background computation finishes.
		// ����̨�������ʱ������ UI�̡߳���̨��������Ϊһ���������ݵ��ⲽ��
		protected void onPostExecute(Void result) {
			Log.i("debug", "onPostExecute");
			btnStop.setEnabled(false);
			btnStart.setEnabled(true);
			btnPlay.setEnabled(true);
			btnFinish.setEnabled(false);
		}

		// ����¼��
		//  invoked on the UI thread before the task is executed. 
		// ��UI�߳��ϵ������������ִ��,ͨ����������������
		protected void onPreExecute() {
			 stateView.setText("����¼��");
			Log.i("debug", "onPreExecute");
			btnStart.setEnabled(false);
			btnPlay.setEnabled(false);
			btnFinish.setEnabled(false);
			btnStop.setEnabled(true);
		}

	}

	// ����
	/**
	 * 1��StreamType:��AudioManager���м�������������һ����STREAM_MUSIC;
	 * 2��SampleRateInHz����ú�AudioRecordʹ�õ���ͬһ��ֵ 
	 * 3��ChannelConfig��ͬ��
	 * 4��AudioFormat��ͬ�� 
	 * 5��BufferSize��ͨ��AudioTrack�ľ�̬����getMinBufferSize����ȡ
	 * 6��Mode��������AudioTrack.MODE_STREAM��MODE_STATIC�����������ֲ�֮ͬ�������Բ����ĵ�
	 * ������һ����������ָ��ո�¼�����ݱ�����ļ���Ȼ��ʼ���ţ��߶�ȡ�߲���
	 * 
	 */
	class PlayTask extends AsyncTask<Void, Integer, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			isPlaying = true;
			// �����СBufferSize��ͨ��AudioTrack�ľ�̬����getMinBufferSize����ȡ
			int bufferSize = AudioTrack.getMinBufferSize(sampleRateInHz,
					channelConfig, audioFormat);
			short[] buffer = new short[bufferSize / 4];
			// ����������������Ƶд�뵽AudioTrack���У�ʵ�ֲ���
			try {
				DataInputStream dis = new DataInputStream(
						new BufferedInputStream(new FileInputStream(audioFile)));
				// ʵ��AudioTrack
				AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC,
						sampleRateInHz, channelConfig, audioFormat, bufferSize,
						AudioTrack.MODE_STREAM);
				// ��ʼ����
				track.play();
				// ����AudioTrack���ŵ����������ԣ�������Ҫһ�߲���һ�߶�ȡ
				while (isPlaying && dis.available() > 0) {
					int i = 0;
					while (dis.available() > 0 && i < buffer.length) {
						buffer[i] = dis.readShort();
						i++;
					}
					// Ȼ������д�뵽AudioTrack��
					track.write(buffer, 0, buffer.length);
				}
				track.stop();
				// ���Ž���
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

			// stateView.setText("���ڲ���");
			btnStart.setEnabled(false);
			btnStop.setEnabled(false);
			btnPlay.setEnabled(false);
			btnFinish.setEnabled(true);
		}
	}
}