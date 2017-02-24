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
import com.identiv.apduengine.desfire.DesfireUtils;
import com.identiv.apduengine.desfire.apdus.*;
import com.identiv.apduengine.desfire.crypto.JceAesCryptor;
import com.identiv.apduengine.engine.StatusResponse;
import com.identiv.apduengine.engine.util.Hex;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

import static com.identiv.apduengine.ui.TsCardExplorerController.*;

/**
 * FXML Controller class
 */
public class ChangePiccKeyController implements Initializable {

    @FXML
    private Button btOK;
    @FXML
    private RadioButton rbNoKeyDiversification;
    @FXML
    private ToggleGroup tgldiversification;
    @FXML
    private RadioButton rbCmacAesKeyDiversification;
    @FXML
    private TextField edDiversificationKey;
    @FXML
    private ComboBox cbTypes;
    @FXML
    private TextField edNoDivKey;

    private ApduSession session;
    private ObservableList historyData;
    private boolean curr_useKeyDiv;
    private String curr_key;
    private boolean curr_isAES;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbTypes.getItems().addAll(OPT_2K3DES, OPT_3K3DES, OPT_AES);
        cbTypes.setValue(OPT_2K3DES);
        disableTextControl4Key();
        setupUI();
    }    

    @FXML
    private void onBtnOkClicked(ActionEvent event) {
        try {
            SmartcardIoTransmitter transmitter = SmartcardIoTransmitter.create();
            String uid = getParamValueByKey(session, DesfireGetCardUID.CARD_UID_KEY);
            String aid = getParamValueByKey(session, SelectTsApplication.AID_KEY);
            String action = "ChangeKey";

            boolean useKeyDiv = !rbCmacAesKeyDiversification.isDisable()
                    && rbCmacAesKeyDiversification.isSelected();

            String newKey = useKeyDiv ? edDiversificationKey.getText().trim()
                    : edNoDivKey.getText().trim();

            if ((newKey == null || newKey.isEmpty()) && cbTypes.getValue() != "AES") {
                session.nextCommands(new SelectPicc(),
                    Utils.buildAuthPiccCmd(curr_useKeyDiv, curr_isAES, uid, curr_key),
                    new ResetTo2K3DesKey()
                );
                action = "ResetTo2K3DesKey";
            }
            else if (useKeyDiv) {
                session.nextCommands(new SelectPicc(),
                    Utils.buildAuthPiccCmd(curr_useKeyDiv, curr_isAES, uid, curr_key),
                    new ChangeKey(DesfireUtils.cmacKeyDivAes128(
                          Hex.decode(uid),
                          new JceAesCryptor(Hex.decode(newKey))))
                );
            } else {
                session.nextCommands(new SelectPicc(),
                    Utils.buildAuthPiccCmd(curr_useKeyDiv, curr_isAES, uid, curr_key),
                    new ChangeKey(Utils.buildKeyAesNoDiv(newKey))
                );
            }
            StatusResponse status = session.transmit(transmitter);
            String receivedData = "AID: " + aid
                    + "; SessionKey: " + getParamValueByKey(session, DesfireAuth.SESSION_KEY);
            historyData.add(0, new HistoryModel(
                status.getStatus().name(),//status
                action,//action
                uid,//uid
                receivedData));//received data
        } catch (Exception e) {
        }
    }

    @FXML
    private void onRdDES(ActionEvent event) {
        disableTextControl4Key();
    }

    @FXML
    private void rdAes(ActionEvent event) {
        disableTextControl4Key();
    }

    @FXML
    private void onCBtextChanged(ActionEvent event) {
        setupUI();
    }

    private void disableTextControl4Key() {
        edDiversificationKey.setDisable(!rbCmacAesKeyDiversification.isSelected());
        edNoDivKey.setDisable(!rbNoKeyDiversification.isSelected()
                || cbTypes.getValue() != "AES");
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
        disableTextControl4Key();
    }

    void setSession(ApduSession session, ObservableList<HistoryModel> historyData,
            boolean curr_useKeyDiv, String curr_key, boolean curr_isAES) {
        this.session = session;
        this.historyData = historyData;
        this.curr_useKeyDiv = curr_useKeyDiv;
        this.curr_key = curr_key;
        this.curr_isAES = curr_isAES;
    }
}
