#1.创建一个File对象,由于存储拍摄后的照片存放地址
		由于Android6.0的权限 读取和写入SD卡是危险权限 为了安全且使用不那么麻烦 
		这里的存放目录就放在cache目录下
		 //从缓存目录中创建一个file  用来保存照片
            File cameraFile = new File(getCacheDir(), "output_img.jpg");
            try {
                if (cameraFile.exists()) {
                    cameraFile.delete();
                }
                cameraFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
#2.由于Android7.0的新特性 访问本地的File无法使用 Uri.fromFile(xxx)
		 //android 中 7.0以后 获取本地file 需要权限 通过FileProvider 基本类似内容提供者 需要xml中配置Provider
            if (Build.VERSION.SDK_INT >= 24) {
				//这里的参数1是Context,参数2是配置manifests中的Privider的author节点,参数3是路径
                photoUri = FileProvider.getUriForFile(this, "com.example.chenlong.cameraalbumtest.photoprovider", cameraFile);
            } else {
                photoUri = Uri.fromFile(cameraFile);
            }
#3.Intent隐式意图跳转
		 //通过意图打开照相机
            Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);	//指定照片存放到参数2的路径
            startActivityForResult(intent, 1);
#4.onActivityResult接收相机返回的图片
			@Override
			protected void onActivityResult(int requestCode, int resultCode, Intent data)
			{
				super.onActivityResult(requestCode, resultCode, data);
				switch (requestCode) {
					case 1:
						//判断相机页面返回的是否是确定 而不是取消
						if (resultCode== RESULT_OK) {
							try {
								Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(photoUri));
								mPhotoImage.setImageBitmap(bitmap);
							} catch (FileNotFoundException e) {
								e.printStackTrace();
							}
						}
						break;
				}
			}
#5.以上为逻辑代码 接下来要去manifests配置FileProvider才能运行

     最好加上权限 为了兼容性
			 <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"></uses-permission>
				<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"></uses-permission>
		在Application节点里配置Provider
			 <!--android:name 属性的值是固定的-->
			<provider
				android:name="android.support.v4.content.FileProvider"
				android:authorities="com.example.chenlong.cameraalbumtest.photoprovider"
				android:exported="false"
				android:grantUriPermissions="true">
				<meta-data
					android:name="android.support.FILE_PROVIDER_PATHS"
					android:resource="@xml/file_paths"></meta-data>	//指定了Provider提供的共享的文件路径 新建一个res包下的xml包
			</provider>
      
#6.配置xml
				<?xml version="1.0" encoding="utf-8"?>
				<paths>
					<!--
					其中name可以是随便填写的
					   path如果为空 表示所有的sd路径  也可以在里面填写绝对路径
					 -->
					<root-path
						name="my_images"
						path=""/>
				</paths>
		我这里配置的root-path的节点 与郭霖的第一行代码有点出入 我用7.0手机无法运行报错 简书的解决方案是需要这个root-path  而不是external-path
    
#调用相册应该有很多很好的三方控件 这里只是简单的调用 具体的直接看demo
