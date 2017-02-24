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

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class CardInfoController implements Initializable {

    private CardInfoModel cardInfoModel;
    @FXML
    private Button btClose;
    @FXML
    private Label lbHVendorId;
    @FXML
    private Label lbHType;
    @FXML
    private Label lbHSubtype;
    @FXML
    private Label lbHMajorVer;
    @FXML
    private Label lbHMinorVer;
    @FXML
    private Label lbHStorageSize;
    @FXML
    private Label lbHCommProType;
    @FXML
    private Label lbSCommProType;
    @FXML
    private Label lbSStorageSize;
    @FXML
    private Label lbSMinorVer;
    @FXML
    private Label lbSMajorVer;
    @FXML
    private Label lbSSubtype;
    @FXML
    private Label lbSType;
    @FXML
    private Label lbSVendorId;
    @FXML
    private Label lbUid;
    @FXML
    private Label lbProdBatchNum;
    @FXML
    private Label lbCalWeekProd;
    @FXML
    private Label lbYearProd;
    private Stage stage;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }

    public void setCardInfoModel(CardInfoModel cardInfoModel) {
        this.cardInfoModel = cardInfoModel;

        lbUid.setText(cardInfoModel.getUid());
        lbProdBatchNum.setText(cardInfoModel.getProdBatchNumber());
        lbCalWeekProd.setText(cardInfoModel.getCalWeekOfProd());
        lbYearProd.setText(cardInfoModel.getYearOfProduction());

        lbHVendorId.setText(cardInfoModel.gethVendorId());
        lbHType.setText(formatDisplayHex(cardInfoModel.gethType()));
        lbHSubtype.setText(formatDisplayHex(cardInfoModel.gethSubtype()));
        lbHMajorVer.setText(cardInfoModel.gethMajorVer());
        lbHMinorVer.setText(cardInfoModel.gethMinorVer());
        lbHStorageSize.setText(formatDisplayHex(cardInfoModel.gethStorageSize()));
        lbHCommProType.setText(formatDisplayHex(cardInfoModel.gethCommProtocalType()));

        lbSVendorId.setText(cardInfoModel.getsVendorId());
        lbSType.setText(formatDisplayHex(cardInfoModel.getsType()));
        lbSSubtype.setText(formatDisplayHex(cardInfoModel.getsSubtype()));
        lbSMajorVer.setText(cardInfoModel.getsMajorVer());
        lbSMinorVer.setText(cardInfoModel.getsMinorVer());
        lbSStorageSize.setText(formatDisplayHex(cardInfoModel.getsStorageSize()));
        lbSCommProType.setText(formatDisplayHex(cardInfoModel.getsCommProtocalType()));
    }

    @FXML
    private void onCloseAction(ActionEvent event) {
        stage.close();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private String formatDisplayHex(String value) {
        if (value != null && !value.isEmpty()) {
            return "0x" + value;
        }
        return value;
    }

}
