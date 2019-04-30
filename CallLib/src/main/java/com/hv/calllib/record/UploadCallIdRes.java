package com.hv.calllib.record;

import com.hv.imlib.protocol.http.HttpRes;

/**
 * Created by zhenhuiyang on 2018/7/2:15:52.
 * Holoview studio.
 */
public class UploadCallIdRes extends HttpRes{

    /**
     * result : {"message":"Ok"}
     */

    private ResultBean result;

    public ResultBean getResult() {
        return result;
    }

    public void setResult(ResultBean result) {
        this.result = result;
    }

    public static class ResultBean {
        /**
         * message : Ok
         */

        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
