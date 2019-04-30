package com.hv.calllib.ar.render;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class StrokedRectangle {

    public enum Type {
        FACE, STANDARD, EXTENDED, TRACKING_3D
    }

    private final static String TAG = "StrokedRectangle";

    String mVertexShaderCode =
            "attribute vec2 v_position;" +
            "uniform mat4 Projection;" +
            "void main()" +
            "{\n" +
            " vec4 pos = vec4(v_position, 0.0, 1.0);\n"+
            "gl_Position = Projection * pos;\n" +
            "}\n";

    String mFragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 uColor ;\n" +
                    "void main()" +
                    "{" +
                    "  gl_FragColor = uColor;\n " +
                    "}";

    private int mAugmentationProgram  = -1;
    private int mPositionSlot;
    private int mColorSlot;
    private int mMatrixModel;

    private int layoutWidth = 640;
    private int layoutHeight = 480;

    static float sRectVerts[] = {
            0, 0, // Draw rectangle outline
            0, 1,
            1, 1,
            1, 0};

    static float sRectVerts3D[] = {
            -1, -1, 0.0f,
            -1,  1, 0.0f,
            1,  1, 0.0f,
            1, -1, 0.0f };

    static float sRectVertsExtended[] = {
            -0.7f, -0.7f, 0.0f,
            -0.7f,  0.7f, 0.0f,
            0.7f,  0.7f, 0.0f,
            0.7f, -0.7f, 0.0f };

    static float sRectVertsFace[] = {
            -0.5f, -0.5f, 0.0f,
            -0.5f,  0.5f, 0.0f,
            0.5f,  0.5f, 0.0f,
            0.5f, -0.5f, 0.0f };


    private ShortBuffer mIndicesBuffer;
    private FloatBuffer mRectBuffer;
    private int     mPenlineWidth  = 5;
    private float[] colorArray = new float[4];

    private final short mIndices[] = { 0, 1, 2, 3 };

    public StrokedRectangle() {
        this(Type.STANDARD, Color.GREEN,5);
    }

    public StrokedRectangle(Type type,int color,int penwidth ) {
        mPenlineWidth = penwidth;
        initColor(color);

        ByteBuffer dlb = ByteBuffer.allocateDirect(mIndices.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        mIndicesBuffer = dlb.asShortBuffer();
        mIndicesBuffer.put(mIndices);
        mIndicesBuffer.position(0);

        ByteBuffer bb = ByteBuffer.allocateDirect(sRectVerts.length * 4);
        bb.order(ByteOrder.nativeOrder());
        mRectBuffer = bb.asFloatBuffer();
        if (type == Type.EXTENDED) {
            mRectBuffer.put(sRectVertsExtended);
        } else if (type == Type.FACE || type == Type.TRACKING_3D) {
            mRectBuffer.put(sRectVertsFace);
        } else {
            mRectBuffer.put(sRectVerts);
        }
        mRectBuffer.position(0);
    }

    private void initColor(int color) {
        float alpha = ((color >>> 24) & 0xFF) / 255f * getAlpha();
        float red = ((color >>> 16) & 0xFF) / 255f * alpha;
        float green = ((color >>> 8) & 0xFF) / 255f * alpha;
        float blue = (color & 0xFF) / 255f * alpha;
        colorArray[0] = red;
        colorArray[1] = green;
        colorArray[2] = blue;
        colorArray[3] = alpha;
    }

    public float getAlpha() {
        return 1f;
    }

    private void setColor() {

        boolean blendingEnabled = (colorArray[3] < 1f);
        enableBlending(blendingEnabled);
        if (blendingEnabled) {
            GLES20.glBlendColor(colorArray[0], colorArray[1], colorArray[2], colorArray[3]);
        }

        GLES20.glUniform4fv(mColorSlot, 1, colorArray, 0);
    }

    private void enableBlending(boolean enableBlending) {
        if (enableBlending) {
            GLES20.glEnable(GLES20.GL_BLEND);
        } else {
            GLES20.glDisable(GLES20.GL_BLEND);
        }
    }

    private void setMatrix(float x, float y, float width, float height) {
        float[] mTempMatrix = new float[32];
        float[] mViewMatrix = new float[32];
        float[] mMatrices = new float[8 * 16];
        float[] mProjectionMatrix = new float[16];

        Matrix.setIdentityM(mMatrices,0);
        Matrix.translateM(mTempMatrix, 0, mMatrices, 0, x, y, 0f);
        Matrix.scaleM(mTempMatrix,0,width,height,1f);

        //float ratio = 1.3333f;
        Matrix.orthoM(mProjectionMatrix, 0, 0, layoutWidth, 0, layoutHeight, -1, 1);
        //Matrix.setLookAtM(mViewMatrix, 0, 60, 60, 7, 60, 60, 0, 0, 1, 0);

        Matrix.multiplyMM(mTempMatrix, 16, mProjectionMatrix, 0, mTempMatrix, 0);

        GLES20.glUniformMatrix4fv( mMatrixModel, 1, false, mTempMatrix, 16);
    }

    public void onDrawFrame(float x,float y,float width,float height ) {
        if (mAugmentationProgram == -1) {
            compileShaders();
            mPositionSlot = GLES20.glGetAttribLocation(mAugmentationProgram, "v_position");
            mColorSlot    = GLES20.glGetUniformLocation(mAugmentationProgram,"uColor");
            mMatrixModel  = GLES20.glGetUniformLocation(mAugmentationProgram, "Projection");

            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            GLES20.glLineWidth(mPenlineWidth);
        }

        //GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        //GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glUseProgram(mAugmentationProgram);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

        GLES20.glVertexAttribPointer(mPositionSlot, 2, GLES20.GL_FLOAT, false, 0, mRectBuffer);
        GLES20.glEnableVertexAttribArray(mPositionSlot);

        setMatrix(x, y, width, height);

        setColor( );

        //GLES20.glViewport(0, 0, layoutWidth, layoutHeight);
        GLES20.glDrawElements(GLES20.GL_LINE_LOOP, mIndices.length, GLES20.GL_UNSIGNED_SHORT, mIndicesBuffer);

        //GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        //GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private void compileShaders() {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, mVertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, mFragmentShaderCode);
        mAugmentationProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mAugmentationProgram, vertexShader);
        GLES20.glAttachShader(mAugmentationProgram, fragmentShader);
        GLES20.glLinkProgram(mAugmentationProgram);
    }

    public static int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);

        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

    public void updateLayout( int width,int height) {
        layoutWidth  = width;
        layoutHeight = height;
    }
}
