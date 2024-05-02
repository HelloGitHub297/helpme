package com.example.capstonedesign;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final List<AudioFile> mAudioFiles = new ArrayList<>();
    private DatabaseReference mDatabaseRef;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listViewAudios = findViewById(R.id.listViewAudios);

        // Firebase Realtime Database의 "voice_files" 노드에 접근합니다.
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("voice_files");

        // "voice_files" 노드의 데이터를 가져오는 리스너를 추가합니다.
        mDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // 데이터가 변경될 때마다 호출됩니다.
                mAudioFiles.clear(); // 목록을 초기화합니다.

                // "voice_files" 노드의 각 자식에 대해 반복합니다.
                for (DataSnapshot fileSnapshot : dataSnapshot.getChildren()) {
                    // 파일명과 URL을 가져와서 AudioFile 객체를 생성하고 목록에 추가합니다.
                    String fileName = fileSnapshot.getKey();
                    String fileUrl = fileSnapshot.child("file_url").getValue(String.class);
                    AudioFile audioFile = new AudioFile(fileName, fileUrl);
                    mAudioFiles.add(audioFile);
                }

                // 목록을 표시하기 위해 ArrayAdapter를 설정합니다.
                ArrayAdapter<AudioFile> adapter = new ArrayAdapter<>(MainActivity.this,
                        android.R.layout.simple_list_item_1, mAudioFiles);
                listViewAudios.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // 데이터 가져오기가 실패한 경우 호출됩니다.
                Toast.makeText(MainActivity.this, "Failed to retrieve audio list", Toast.LENGTH_SHORT).show();
            }
        });

        // 목록에서 항목을 선택할 때마다 해당 파일의 URL을 확인할 수 있는 다이얼로그를 표시합니다.
        listViewAudios.setOnItemClickListener((adapterView, view, position, id) -> {
            AudioFile selectedAudio = mAudioFiles.get(position);
            String selectedAudioUrl = selectedAudio.getUrl();
            // 선택된 파일의 URL을 사용하여 AlertDialog를 표시합니다.
            showUrlDialog(selectedAudioUrl);
        });
    }

    // 선택된 파일의 URL을 보여주는 다이얼로그를 표시합니다.
    private void showUrlDialog(String audioUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Audio URL");
        builder.setMessage(audioUrl);
        builder.setPositiveButton("재생", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 선택된 파일의 URL을 사용하여 음성 파일을 재생합니다.
                playAudio(audioUrl);
            }
        });
        builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // 선택된 URL의 음성 파일을 재생합니다.
    // 선택된 URL의 음성 파일을 재생합니다.
    private void playAudio(String audioUrl) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());
        try {
            mediaPlayer.setDataSource(audioUrl);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start(); // 재생이 준비되었을 때 자동으로 시작합니다.
                    Toast.makeText(MainActivity.this, "오디오 재생 성공", Toast.LENGTH_SHORT).show();
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Toast.makeText(MainActivity.this, "오디오 재생 실패", Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "오디오 재생 실패", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // AudioFile 클래스는 파일 이름과 URL을 저장하는 클래스입니다.
    private static class AudioFile {
        private String name;
        private String url;

        public AudioFile(String name, String url) {
            this.name = name;
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
