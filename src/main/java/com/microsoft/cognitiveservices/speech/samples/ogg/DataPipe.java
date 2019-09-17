//MIT License
//
//Copyright (c) Microsoft Corporation. All rights reserved.
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE

package com.microsoft.cognitiveservices.speech.samples.ogg;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DataPipe implements Runnable {
    private final InputStream is;
    private final OutputStream os;

    DataPipe(InputStream is, OutputStream os) {
        this.is = is;
        this.os = os;
    }

    public void run() {
        byte buffer[] = new byte[1024 * 1024];
        int numRead;
        try {
            while ((numRead = this.is.read(buffer)) != -1) {
                os.write(buffer, 0, numRead);
            }
            this.os.close();
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

    public static Thread start(InputStream is, OutputStream os) {
        Thread t = new Thread(new DataPipe(is, os));
        t.start();
        return t;
    }
}
