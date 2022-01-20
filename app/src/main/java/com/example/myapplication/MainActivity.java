package com.example.myapplication;


import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSource;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // creating a variable for exoplayerview.
    PlayerView exoPlayerView;
    ExoPlayer exoPlayer;
    String mp4Video = "asset:///file_example_MP4_480_1_5MG.mp4";
    String mp4Video2 = "asset:///sample-avi-file.mp4";

    String aviVideo = "asset:///avc1/erdeavc.avi";
    String aviVideo2 = "asset:///mp42/sample-avi-file.avi";
    String kamera1_h264 = "asset:///h264/kamera1.avi";
    String aviVideo3 = "asset:///h264/video.avi";
    String ocean = "asset:///h264/ocean.avi";
    String erde = "asset:///h264/erde.avi";
    String kamera_h264 = "asset:///h264/kamera.avi";
    String kamera2_h264 = "asset:///h264/kamera2.avi";
    String kamera3_h264 = "asset:///h264/kamera3.avi";

    // url of video which we are loading.
    String videoURL =
            "https://media.geeksforgeeks.org/wp-content/uploads/20201217163353/Screenrecorder"
            + "-2020-12-17-16-32-03-350.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        exoPlayerView = findViewById(R.id.idExoPlayerVIew);
        try {

            // bandwisthmeter is used for
            // getting default bandwidth
            BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter.Builder(this).build();

            // track selector is used to navigate between
            // video using a default seekbar.
            //TrackSelector trackSelector = new DefaultTrackSelector(this, new
            // AdaptiveTrackSelection.Factory().);

            // we are adding our track selector to exoplayer.
            //exoPlayer = new ExoPlayer.Builder(this).setTrackSelector(trackSelector).build();


            // we are parsing a video url
            // and parsing its video uri.
            Uri videouri = Uri.parse(videoURL);

            // we are creating a variable for datasource factory
            // and setting its user agent as 'exoplayer_view'
            DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this);

            // we are creating a variable for extractor factory
            // and setting it to default extractor factory.
            ExtractorsFactory extractorsFactory = new ExtractorsFactory() {
                @Override
                public Extractor[] createExtractors() {
                    Extractor[] extractors = new Extractor[2];
                    extractors[0] = new Mp4Extractor();
                    extractors[1] = new AviExtractor();
                    return extractors;
                }
            };

            // we are creating a media source with above variables
            // and passing our event handlerkamera2_h264 as null,
            MediaItem mediaItem =
                    MediaItem.fromUri(Uri.parse(kamera3_h264));

            MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory,
                                                                         extractorsFactory).createMediaSource(mediaItem);

            // we are preparing our exoplayer
            // with media source.

            exoPlayer = new ExoPlayer.Builder(exoPlayerView.getContext()).build();
            exoPlayer.setMediaSource(mediaSource);
            exoPlayer.prepare();
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
            // we are setting our exoplayer
            // when it is ready.
            exoPlayer.setPlayWhenReady(true);
            exoPlayerView.setPlayer(exoPlayer);
        } catch (Exception e) {
            // below line is used for
            // handling our errors.
            Log.e("TAG", "Error : " + e.toString());
        }
    }
}