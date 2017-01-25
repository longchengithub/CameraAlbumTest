package com.example.chenlong.cameraalbumtest;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.btn_photo)
    Button mTakePhoto;
    @BindView(R.id.btn_choose)
    Button mChoosePhoto;
    @BindView(R.id.iv_photo)
    ImageView mPhotoImage;

    private Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        /**
         * 打开照相机
         */
        mTakePhoto.setOnClickListener(v -> {
            //1.从缓存目录中创建一个file  用来保存照片
            File cameraFile = new File(getCacheDir(), "output_img.jpg");
            try {
                if (cameraFile.exists()) {
                    cameraFile.delete();
                }
                cameraFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //2.android 中 7.0以后 获取本地file 需要权限 通过FileProvider 基本类似内容提供者 需要xml中配置Provider
            if (Build.VERSION.SDK_INT >= 24) {
                //这里的参数1是Context,参数2是配置manifests中的Privider的author节点,参数3是路径
                photoUri = FileProvider.getUriForFile(this, "com.example.chenlong.cameraalbumtest.photoprovider", cameraFile);
            } else {
                photoUri = Uri.fromFile(cameraFile);
            }

            //3.通过意图打开照相机
            Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);     //指定照片存放到参数2的路径
            startActivityForResult(intent, 1);
        });

        /**
         * 打开相册
         */
        mChoosePhoto.setOnClickListener(v -> {
            //高版本中读取sd卡需要用到运行时权限 在请求完毕权限后的回调打开相册
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            } else {
                //打开相册
                openAlbum();
            }
        });
    }

    /**
     * 申请运行时权限的回调
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openAlbum();
                } else {
                    Toast.makeText(this, "亲!我需要权限才能打开相册啊", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    /**
     * 隐式意图打开相册
     */
    private void openAlbum()
    {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, 2);
    }

    /**
     * 选择图片或照相返回的回调
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                //判断相机页面返回的是否是确定 而不是取消
                if (resultCode == RESULT_OK) {
                    try {
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(photoUri));
                        mPhotoImage.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case 2:
                if (resultCode == RESULT_OK) {
                    //判断手机的系统版本 4.4以上的处理方案有变
                    if (Build.VERSION.SDK_INT >= 19) {
                        handleImageOnKitKat(data);
                    } else {
                        handleImageBeforeOnKitKat(data);
                    }
                }
                break;
        }
    }

    /**
     * api19以下的处理方案
     * @param data
     */
    private void handleImageBeforeOnKitKat(Intent data)
    {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        displayImage(imagePath);
    }

    /**
     * api19以上的处理方案
     *
     * @param data
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void handleImageOnKitKat(Intent data)
    {
        String imagePath = null;
        Uri uri = data.getData();
        //判断返回的uri的类型
        if (DocumentsContract.isDocumentUri(this, uri)) {
            //如果是Document类型
            String docId = DocumentsContract.getDocumentId(uri);
            //又是一层的判断
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];    //解析返回的uri:后面的真正的id
                String selection = MediaStore.Images.Media._ID + "=" + id; //拼接一个sql语句查询条件
                //定义一个查询方法  传入2个参数 参数1表示系统images数据库的uri 第二个是根据id的数据库查询条件
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            //如果是content类型
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            //如果是file类型
            imagePath = uri.getPath();
        }

        displayImage(imagePath);
    }

    /**
     * 拿到路径后 加载图片到控件上
     *
     * @param imagePath
     */
    private void displayImage(String imagePath)
    {
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            mPhotoImage.setImageBitmap(bitmap);
        } else {
            Toast.makeText(this, "获取图片失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 通过内容解析者 根据不同的uri类型 访问不同的数据库 不同的查询条件 拿到图片地址
     *
     * @param uri
     * @param selection
     * @return
     */
    private String getImagePath(Uri uri, String selection)
    {
        String path = null;
        //通过内容提供者查询
        Cursor query = getContentResolver().query(uri, null, selection, null, null);
        //通过uri和selection获取图片的真实路径
        if (query != null) {
            if (query.moveToNext()) {
                path = query.getString(query.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            query.close();
        }
        return path;
    }
}
