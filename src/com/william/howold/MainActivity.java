package com.william.howold;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facepp.error.FaceppParseException;

public class MainActivity extends Activity implements OnClickListener {

    private Bitmap mPhotoImg;
    private static final int PICK_CODE = 0x110;
    private static final int MSG_SUCCESS = 0x111;
    private static final int MSG_ERROR = 0x112;
    private ImageView mPhoto;
    private Button getImg, detect;
    private View waiting;
    private String currentPhotoPath;
    private Paint mPaint;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            waiting.setVisibility(View.GONE);
            switch (msg.what) {
            case MSG_SUCCESS:
                JSONObject result = (JSONObject) msg.obj;
                prepareRsBitMap(result);
                mPhoto.setImageBitmap(mPhotoImg);
                break;
            case MSG_ERROR:
                String emsg = (String) msg.obj;
                Toast.makeText(MainActivity.this, emsg, Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
            }
        }

    };

    private void prepareRsBitMap(JSONObject result) {
        Bitmap bitmap = Bitmap.createBitmap(mPhotoImg.getWidth(), mPhotoImg.getHeight(), mPhotoImg.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(mPhotoImg, 0, 0, null);
        try {
            JSONArray faceArray = result.getJSONArray("face");
            int faceCount = faceArray.length();
            for (int i = 0; i < faceCount; i++) {
                JSONObject face = faceArray.getJSONObject(i);
                JSONObject position = face.getJSONObject("position");
                float x = (float) position.getJSONObject("center").getDouble("x");
                float y = (float) position.getJSONObject("center").getDouble("y");
                float width = (float) position.getDouble("width");
                float height = (float) position.getDouble("height");
                x = x / 100 * bitmap.getWidth();
                y = y / 100 * bitmap.getWidth();
                width = width / 100 * bitmap.getWidth();
                height = height / 100 * bitmap.getWidth();
                mPaint.setColor(Color.WHITE);
                mPaint.setStrokeWidth(3);
                canvas.drawLine(x - width / 2, y - height / 2, x - width / 2, y + height / 2, mPaint);
                canvas.drawLine(x - width / 2, y - height / 2, x + width / 2, y - height / 2, mPaint);
                canvas.drawLine(x + width / 2, y - height / 2, x + width / 2, y + height / 2, mPaint);
                canvas.drawLine(x - width / 2, y + height / 2, x + width / 2, y + height / 2, mPaint);
                int age = face.getJSONObject("attribute").getJSONObject("age").getInt("value");
                int range = face.getJSONObject("attribute").getJSONObject("age").getInt("range");
                age = age + range;
                String sex = face.getJSONObject("attribute").getJSONObject("gender").getString("value");
                boolean isMale = "Male".equals(sex);
                Bitmap ageBitmap = buildAgeBitMap(age, isMale);
                int ageW = ageBitmap.getWidth();
                int ageH = ageBitmap.getHeight();
                if (bitmap.getWidth() < mPhoto.getWidth() && bitmap.getHeight() < mPhoto.getHeight()) {
                    float ratio = Math.max(bitmap.getWidth() * 1.0f / mPhoto.getWidth(), bitmap.getHeight() * 1.0f / mPhoto.getHeight());
                    ageBitmap = Bitmap.createScaledBitmap(ageBitmap, (int) (ageW * ratio), (int) (ageH * ratio), false);
                }
                canvas.drawBitmap(ageBitmap, x - ageBitmap.getWidth() / 2, y - height / 2 - ageBitmap.getHeight(), null);
                mPhotoImg = bitmap;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    };

    private Bitmap buildAgeBitMap(int age, boolean isMale) {
        TextView tv = (TextView) findViewById(R.id.id_age_gender);
        tv.setText(String.valueOf(age));
        if (isMale) {
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.male), null, null, null);
        } else {
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.female), null, null, null);
        }
        tv.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(tv.getDrawingCache());
        tv.destroyDrawingCache();
        return bitmap;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initEvents();
        mPaint = new Paint();
    }

    private void initEvents() {
        getImg.setOnClickListener(this);
        detect.setOnClickListener(this);

    }

    private void initViews() {
        mPhoto = (ImageView) findViewById(R.id.imgArea);
        getImg = (Button) findViewById(R.id.getImg);
        detect = (Button) findViewById(R.id.detect);
        waiting = findViewById(R.id.waiting);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.getImg:
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_CODE);
            break;
        case R.id.detect:
            waiting.setVisibility(View.VISIBLE);
            if (currentPhotoPath == null) {
                mPhotoImg = BitmapFactory.decodeResource(getResources(), R.drawable.t4);
            }
            FaceDetectUtil.detect(mPhotoImg, new FaceDetectUtil.CallBack() {
                @Override
                public void success(JSONObject reslut) {
                    Message msg = Message.obtain();
                    msg.what = MSG_SUCCESS;
                    msg.obj = reslut;
                    handler.sendMessage(msg);
                }
                
                @Override
                public void error(FaceppParseException e) {
                    Message msg = Message.obtain();
                    msg.what = MSG_ERROR;
                    msg.obj = e.getErrorMessage();
                    handler.sendMessage(msg);
                }
            });
            break;
        default:
            break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_CODE) {
            if (data != null) {
                Uri uri = data.getData();
                Cursor c = getContentResolver().query(uri, null, null, null, null);
                c.moveToFirst();
                int index = c.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                currentPhotoPath = c.getString(index);
                c.close();
                resizePhoto();
                mPhoto.setImageBitmap(mPhotoImg);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void resizePhoto() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(currentPhotoPath, options);
        double ratio = Math.max(options.outWidth * 1.0d / 1024f, options.outHeight * 1.0d / 1024f);
        options.inSampleSize = (int) Math.ceil(ratio);
        options.inJustDecodeBounds = false;
        mPhotoImg = BitmapFactory.decodeFile(currentPhotoPath, options);
    }
}
