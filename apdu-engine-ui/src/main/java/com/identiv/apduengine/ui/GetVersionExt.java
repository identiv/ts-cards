/**
 * Copyright 2017 Identiv
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.identiv.apduengine.ui;

import com.identiv.apduengine.desfire.apdus.DesfireGetCardUID;
import com.identiv.apduengine.desfire.apdus.GetVersion;
import com.identiv.apduengine.engine.ApduResponse;

public class GetVersionExt extends GetVersion {
    public static final String PROD_BATCH_NUMBER = "PROD_BATCH_NUMBER";
    public static final String CAL_WEEK_PROD = "CAL_WEEK_PROD";
    public static final String YEAR_PRODUCTION = "YEAR_PRODUCTION";
    //First frame contains hardware related info
    public static final String H_VENDOR_ID = "H_VENDOR_ID";
    public static final String H_TYPE = "H_TYPE";
    public static final String H_SUBTYPE = "H_SUBTYPE";
    public static final String H_MAJOR_VER = "H_MAJOR_VER";
    public static final String H_MINOR_VER = "H_MINOR_VER";
    public static final String H_STORAGE_SIZE = "H_STORAGE_SIZE";
    public static final String H_COMM_PROTOCAL_TYPE = "H_COMM_PROTOCAL_TYPE";
    //Second frame contains software related info
    public static final String S_VENDOR_ID = "S_VENDOR_ID";
    public static final String S_TYPE = "S_TYPE";
    public static final String S_SUBTYPE = "S_SUBTYPE";
    public static final String S_MAJOR_VER = "S_MAJOR_VER";
    public static final String S_MINOR_VER = "S_MINOR_VER";
    public static final String S_STORAGE_SIZE = "S_STORAGE_SIZE";
    public static final String S_COMM_PROTOCAL_TYPE = "S_COMM_PROTOCAL_TYPE";

    private boolean getFirstFrame = false;
    private boolean getSecondFrame = false;

    @Override
    public void validateResponse(ApduResponse response) {
        if (isDone()) {//is third frame
            byte[] uid = new byte[7];
            byte[] proBatchNumber = new byte[5];
            byte[] calWeekProd = new byte[1];
            byte[] yearProd = new byte[1];
            System.arraycopy(response.getData(), 0, uid, 0, uid.length);
            System.arraycopy(response.getData(), 7, proBatchNumber, 0, proBatchNumber.length);
            System.arraycopy(response.getData(),12, calWeekProd, 0, calWeekProd.length);
            System.arraycopy(response.getData(), 13, yearProd, 0, yearProd.length);
            getSession().setParameter(DesfireGetCardUID.CARD_UID_KEY, uid);
            getSession().setParameter(PROD_BATCH_NUMBER, proBatchNumber);
            getSession().setParameter(CAL_WEEK_PROD, calWeekProd);
            getSession().setParameter(YEAR_PRODUCTION, yearProd);
            super.validateResponse(response);
        } else {
            if (!getFirstFrame) {
                getFirstFrame = true;
                byte[] vendorId = new byte[1];
                byte[] type = new byte[1];
                byte[] subtype = new byte[1];
                byte[] majorVer = new byte[1];
                byte[] minorVer = new byte[1];
                byte[] storageSize = new byte[1];
                byte[] commProtocalType = new byte[1];
                System.arraycopy(response.getData(), 0, vendorId, 0, vendorId.length);
                System.arraycopy(response.getData(), 1, type, 0, type.length);
                System.arraycopy(response.getData(), 2, subtype, 0, subtype.length);
                System.arraycopy(response.getData(), 3, majorVer, 0, majorVer.length);
                System.arraycopy(response.getData(), 4, minorVer, 0, minorVer.length);
                System.arraycopy(response.getData(), 5, storageSize, 0, storageSize.length);
                System.arraycopy(response.getData(), 6, commProtocalType, 0, commProtocalType.length);
                getSession().setParameter(H_VENDOR_ID, vendorId);
                getSession().setParameter(H_TYPE, type);
                getSession().setParameter(H_SUBTYPE, subtype);
                getSession().setParameter(H_MAJOR_VER, majorVer);
                getSession().setParameter(H_MINOR_VER, minorVer);
                getSession().setParameter(H_STORAGE_SIZE, storageSize);
                getSession().setParameter(H_COMM_PROTOCAL_TYPE, commProtocalType);
            } else if (!getSecondFrame) {//is second frame
                getSecondFrame = true;
                byte[] vendorId = new byte[1];
                byte[] type = new byte[1];
                byte[] subtype = new byte[1];
                byte[] majorVer = new byte[1];
                byte[] minorVer = new byte[1];
                byte[] storageSize = new byte[1];
                byte[] commProtocalType = new byte[1];
                System.arraycopy(response.getData(), 0, vendorId, 0, vendorId.length);
                System.arraycopy(response.getData(), 1, type, 0, type.length);
                System.arraycopy(response.getData(), 2, subtype, 0, subtype.length);
                System.arraycopy(response.getData(), 3, majorVer, 0, majorVer.length);
                System.arraycopy(response.getData(), 4, minorVer, 0, minorVer.length);
                System.arraycopy(response.getData(), 5, storageSize, 0, storageSize.length);
                System.arraycopy(response.getData(), 6, commProtocalType, 0, commProtocalType.length);                
                getSession().setParameter(S_VENDOR_ID, vendorId);
                getSession().setParameter(S_TYPE, type);
                getSession().setParameter(S_SUBTYPE, subtype);
                getSession().setParameter(S_MAJOR_VER, majorVer);
                getSession().setParameter(S_MINOR_VER, minorVer);
                getSession().setParameter(S_STORAGE_SIZE, storageSize);
                getSession().setParameter(S_COMM_PROTOCAL_TYPE, commProtocalType);
            } else {//is third frame
                byte[] uid = new byte[7];
                byte[] proBatchNumber = new byte[5];
                byte[] calWeekProd = new byte[1];
                byte[] yearProd = new byte[1];
                System.arraycopy(response.getData(), 0, uid, 0, uid.length);
                System.arraycopy(response.getData(), 7, proBatchNumber, 0, proBatchNumber.length);
                System.arraycopy(response.getData(),12, calWeekProd, 0, calWeekProd.length);
                System.arraycopy(response.getData(), 13, yearProd, 0, yearProd.length);
                getSession().setParameter(DesfireGetCardUID.CARD_UID_KEY, uid);
                getSession().setParameter(PROD_BATCH_NUMBER, proBatchNumber);
                getSession().setParameter(CAL_WEEK_PROD, calWeekProd);
                getSession().setParameter(YEAR_PRODUCTION, yearProd);
            }
            super.validateResponse(response);
        }
    }
    
}
