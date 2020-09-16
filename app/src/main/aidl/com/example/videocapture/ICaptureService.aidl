// ICaptureService.aidl
package com.example.videocapture;

// Declare any non-default types here with import statements

interface ICaptureService {
   void pushFD(int fd);
   int pullFD();
}
