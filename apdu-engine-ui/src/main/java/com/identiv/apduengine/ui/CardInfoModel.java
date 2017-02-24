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

/**
 */
public class CardInfoModel {
    private String uid;
    private String prodBatchNumber;
    private String calWeekOfProd;
    private String yearOfProduction;
    //First frame contains hardware related info
    private String hVendorId;
    private String hType;
    private String hSubtype;
    private String hMajorVer;
    private String hMinorVer;
    private String hStorageSize;
    private String hCommProtocalType;
    //Second frame contains software related info
    private String sVendorId;
    private String sType;
    private String sSubtype;
    private String sMajorVer;
    private String sMinorVer;
    private String sStorageSize;
    private String sCommProtocalType;

    public CardInfoModel() {
    }

    public CardInfoModel(String uid, String prodBatchNumber, String calWeekOfProd,
            String yearOfProduction, String hVendorId, String hType, String hSubtype,
            String hMajorVer, String hMinorVer, String hStorageSize,
            String hCommProtocalType, String sVendorId, String sType,
            String sSubtype, String MajorVer, String MinorVer, String sStorageSize,
            String sCommProtocalType) {
        this.uid = uid;
        this.prodBatchNumber = prodBatchNumber;
        this.calWeekOfProd = calWeekOfProd;
        this.yearOfProduction = yearOfProduction;
        this.hVendorId = hVendorId;
        this.hType = hType;
        this.hSubtype = hSubtype;
        this.hMajorVer = hMajorVer;
        this.hMinorVer = hMinorVer;
        this.hStorageSize = hStorageSize;
        this.hCommProtocalType = hCommProtocalType;
        this.sVendorId = sVendorId;
        this.sType = sType;
        this.sSubtype = sSubtype;
        this.sMajorVer = MajorVer;
        this.sMinorVer = MinorVer;
        this.sStorageSize = sStorageSize;
        this.sCommProtocalType = sCommProtocalType;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getProdBatchNumber() {
        return prodBatchNumber;
    }

    public void setProdBatchNumber(String prodBatchNumber) {
        this.prodBatchNumber = prodBatchNumber;
    }

    public String getCalWeekOfProd() {
        return calWeekOfProd;
    }

    public void setCalWeekOfProd(String calWeekOfProd) {
        this.calWeekOfProd = calWeekOfProd;
    }

    public String getYearOfProduction() {
        return yearOfProduction;
    }

    public void setYearOfProduction(String yearOfProduction) {
        this.yearOfProduction = yearOfProduction;
    }

    public String gethVendorId() {
        return hVendorId;
    }

    public void sethVendorId(String hVendorId) {
        this.hVendorId = hVendorId;
    }

    public String gethType() {
        return hType;
    }

    public void sethType(String hType) {
        this.hType = hType;
    }

    public String gethSubtype() {
        return hSubtype;
    }

    public void sethSubtype(String hSubtype) {
        this.hSubtype = hSubtype;
    }

    public String gethMajorVer() {
        return hMajorVer;
    }

    public void sethMajorVer(String hMajorVer) {
        this.hMajorVer = hMajorVer;
    }

    public String gethMinorVer() {
        return hMinorVer;
    }

    public void sethMinorVer(String hMinorVer) {
        this.hMinorVer = hMinorVer;
    }

    public String gethStorageSize() {
        return hStorageSize;
    }

    public void sethStorageSize(String hStorageSize) {
        this.hStorageSize = hStorageSize;
    }

    public String gethCommProtocalType() {
        return hCommProtocalType;
    }

    public void sethCommProtocalType(String hCommProtocalType) {
        this.hCommProtocalType = hCommProtocalType;
    }

    public String getsVendorId() {
        return sVendorId;
    }

    public void setsVendorId(String sVendorId) {
        this.sVendorId = sVendorId;
    }

    public String getsType() {
        return sType;
    }

    public void setsType(String sType) {
        this.sType = sType;
    }

    public String getsSubtype() {
        return sSubtype;
    }

    public void setsSubtype(String sSubtype) {
        this.sSubtype = sSubtype;
    }

    public String getsMajorVer() {
        return sMajorVer;
    }

    public void setsMajorVer(String MajorVer) {
        this.sMajorVer = MajorVer;
    }

    public String getsMinorVer() {
        return sMinorVer;
    }

    public void setsMinorVer(String MinorVer) {
        this.sMinorVer = MinorVer;
    }

    public String getsStorageSize() {
        return sStorageSize;
    }

    public void setsStorageSize(String sStorageSize) {
        this.sStorageSize = sStorageSize;
    }

    public String getsCommProtocalType() {
        return sCommProtocalType;
    }

    public void setsCommProtocalType(String sCommProtocalType) {
        this.sCommProtocalType = sCommProtocalType;
    }
}
