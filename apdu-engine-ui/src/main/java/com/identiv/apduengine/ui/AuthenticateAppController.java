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

import com.identiv.apduengine.ApduSession;
import com.identiv.apduengine.SmartcardIoTransmitter;
import com.identiv.apduengine.desfire.apdus.DesfireAuth;
import com.identiv.apduengine.desfire.apdus.DesfireGetCardUID;
import com.identiv.apduengine.desfire.apdus.SelectTsApplication;
import com.identiv.apduengine.engine.StatusResponse;
import static com.identiv.apduengine.ui.TsCardExplorerController.OPT_2K3DES;
import static com.identiv.apduengine.ui.TsCardExplorerController.OPT_3K3DES;
import static com.identiv.apduengine.ui.TsCardExplorerController.OPT_AES;
import static com.identiv.apduengine.ui.TsCardExplorerController.getParamValueByKey;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

/**
 * FXML Controller class
 */
public class AuthenticateAppController implements Initializable {

    @FXML
    private ComboBox cbTypes;
    @FXML
    private TextField edDiversificationKey;
    @FXML
    private RadioButton rbCmacAesKeyDiversification;
    @FXML
    private ToggleGroup tgldiversification;
    @FXML
    private RadioButton rbNoKeyDiversification;
    @FXML
    private Button btAuthenticate;

    private ApduSession session;
    private ObservableList historyData;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbTypes.getItems().addAll(OPT_2K3DES, OPT_3K3DES, OPT_AES);
        cbTypes.setValue(OPT_2K3DES);
        disableCmcAesKeyControl();
        setupUI();
    }

    private void disableCmcAesKeyControl() {
        edDiversificationKey.setDisable(!rbCmacAesKeyDiversification.isSelected());
    }

    @FXML
    private void onCBtextChanged(ActionEvent event) {
        setupUI();
    }

    @FXML
    private void rdAes(ActionEvent event) {
        disableCmcAesKeyControl();
    }

    @FXML
    private void onRdDES(ActionEvent event) {
        disableCmcAesKeyControl();
    }

    @FXML
    private void onBtnAuthClicked(ActionEvent event) {
        try {
            SmartcardIoTransmitter transmitter = SmartcardIoTransmitter.create();
            String uid = getParamValueByKey(session, DesfireGetCardUID.CARD_UID_KEY);
            String aid = getParamValueByKey(session, SelectTsApplication.AID_KEY);
            boolean useKeyDiv = !rbCmacAesKeyDiversification.isDisable()
                    && rbCmacAesKeyDiversification.isSelected();
            DesfireAuth cmdDesfireAuth = Utils.buildAuthAppCmd(
                    useKeyDiv, uid, aid, edDiversificationKey.getText().trim());
            session.nextCommands(
                cmdDesfireAuth
            );
            StatusResponse status = session.transmit(transmitter);
            String receivedData = "AID: " + aid
                    + "; SessionKey: " + getParamValueByKey(session, DesfireAuth.SESSION_KEY);
            historyData.add(0, new HistoryModel(
                status.getStatus().name(),//status
                "DesfireAuth",//action
                uid,//uid
                receivedData));//received data
        } catch (Exception e) {
        }
    }

    void setSession(ApduSession session, ObservableList<HistoryModel> historyData) {
        this.session = session;
        this.historyData = historyData;
    }

    private void setupUI() {
        if (cbTypes.getValue().equals(OPT_2K3DES)
                || cbTypes.getValue().equals(OPT_3K3DES)) {
            rbNoKeyDiversification.setSelected(true);
            rbNoKeyDiversification.setDisable(false);
            rbCmacAesKeyDiversification.setDisable(true);
        } else {
            rbNoKeyDiversification.setDisable(false);
            rbCmacAesKeyDiversification.setDisable(false);
        }
        disableCmcAesKeyControl();
    }
}
