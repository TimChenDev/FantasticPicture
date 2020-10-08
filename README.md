# FantasticPicture

[![jitpack](https://jitpack.io/v/TimChenDev/FantasticPicture.svg)](https://jitpack.io/#TimChenDev/FantasticPicture)

這是一個圖片取得最基本的範例, 同時具備基本的壓縮, 裁切的工具與示範

## Installation

Add the JitPack repository to your build.gradle file

``` gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

Add the following dependency to your app/build.gradle file:

``` gradle
dependencies {
    implementation 'com.github.TimChenDev:FantasticPicture:1.1.0'
}
```

## How to use

Input a uri, and it will return a fantastic picture

``` kotlin
val bitmap = FantasticPicture.init(this)
                    .pixelLimit(300)
                    .sizeLimit(100)
                    .getFantasticBitmapFromUri(uri)
image.setImageBitmap(bitmap)
```

or you can see the example

## LICENSE

``` text
   Copyright 2017 Google

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

```
