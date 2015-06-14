package com.william.howold;

import java.io.ByteArrayOutputStream;

import org.json.JSONObject;

import android.graphics.Bitmap;
import android.util.Log;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

public class FaceDetectUtil {
    public interface CallBack {
        public void success(JSONObject reslut);

        public void error(FaceppParseException e);
    }

    public static void detect(final Bitmap bitmap, final CallBack callBack) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpRequests reques = new HttpRequests(Consts.key, Consts.screte, true, true);
                    Bitmap bmSmall = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight());
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bmSmall.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] arrays = baos.toByteArray();
                    PostParameters parameters = new PostParameters();
                    parameters.setImg(arrays);
                    JSONObject result = reques.detectionDetect(parameters);
                    System.out.println(result.toString());
                    Log.i("info", result.toString());
                    if (callBack != null) {
                        callBack.success(result);
                    }
                } catch (FaceppParseException e) {
                    if (callBack != null) {
                        callBack.error(e);
                    }
                }

            }
        }).start();
    }

}
