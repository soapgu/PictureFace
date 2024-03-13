package com.soapgu.pictureface;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.space365.face.arcsoft.FaceTask;
import com.space365.face.arcsoft.faceserver.FaceServer;

import java.io.ByteArrayOutputStream;
import java.util.List;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class MainActivity extends AppCompatActivity {
    private FaceTask faceTask;
    private TextView msg;
    private ImageView face_image;

    private SimpleFaceRectView face_rect;
    private int current_id;
    private final CompositeDisposable disposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        msg = findViewById(R.id.msg);
        face_image = findViewById(R.id.face_image);
        face_rect = findViewById(R.id.face_rect);

        faceTask = new FaceTask(this);
        disposable.add( initializeFace().subscribe( t-> Logger.i("initialize %s",t)) );
        findViewById(R.id.btn_find).setOnClickListener( v -> {
            if( current_id != R.drawable.picture1 ){
                current_id = R.drawable.picture1;
            } else {
                current_id = R.drawable.picture2;
            }
            detectFaceByImage();
        } );

        findViewById(R.id.btn_size).setOnClickListener( v -> {
            Logger.i("image width:%s,height:%s",face_image.getWidth(),face_image.getHeight());
        } );
    }

    private void detectFaceByImage(){
        Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), current_id);
        Logger.i("origin width:%s,height:%s",bitmap.getWidth(),bitmap.getHeight());
        bitmap = FaceServer.getInstance().getAlignedBitmap(bitmap);
        face_image.setImageBitmap( bitmap );
        Logger.i("image width:%s",face_image.getHeight());
        List<Rect> rects = FaceServer.getInstance().findFace(bitmap);
        face_rect.updateFaceRect( bitmap.getWidth(),bitmap.getHeight(),rects );
        msg.setText( String.format( "识别人脸:%s",rects.size()) );
    }

    private Single<Boolean> initializeFace(){
        return faceTask.isComplete() ? Single.just(true) : faceTask.run();
    }
}