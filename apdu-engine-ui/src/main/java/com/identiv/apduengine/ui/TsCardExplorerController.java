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
import com.identiv.apduengine.desfire.apdus.GetApplicationIds;
import com.identiv.apduengine.desfire.apdus.GetVersion;
import com.identiv.apduengine.desfire.apdus.SelectApplication;
import com.identiv.apduengine.desfire.apdus.SelectTsApplication;
import com.identiv.apduengine.engine.ApduStatus;
import com.identiv.apduengine.engine.StatusResponse;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.identiv.apduengine.engine.util.Hex;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javax.smartcardio.CardException;

public class TsCardExplorerController implements Initializable {

    @FXML
    private TextField edDiversificationKey;
    @FXML
    private TableView tbViewAppIDs;
    @FXML
    private Button btGetIDs;
    @FXML
    private Button btSelectApp;
    @FXML
    private Button btDeleteApp;
    @FXML
    private TableView tbViewHistory;
    @FXML
    private Button btAuthenticate;
    @FXML
    private Label lbResultTabAuth;
    @FXML
    private Label lbResultTabApp;
    @FXML
    private TableColumn appIdCol;
    @FXML
    private TableColumn selCol;
    @FXML
    private TableColumn statusCol;
    @FXML
    private TableColumn cmdCol;
    @FXML
    private TableColumn receivedDataCol;
    @FXML
    private TableColumn uidCol;
    @FXML
    private ComboBox cbTypes;
    @FXML
    private RadioButton rbNoKeyDiversification;
    @FXML
    private RadioButton rbCmacAesKeyDiversification;
    @FXML
    private ToggleGroup tgldiversification;
    @FXML
    private MenuItem miClear;
    @FXML
    private Button btCardInfo;
    @FXML
    private Button btChangePiccKey;
    @FXML
    private TextField edNoDivKey;

    ObservableList<HistoryModel> historyData = FXCollections.observableArrayList();
    public static final String OPT_2K3DES = "2K3DES";
    public static final String OPT_3K3DES = "3K3DES";
    public static final String OPT_AES = "AES";


    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbTypes.getItems().addAll(OPT_2K3DES, OPT_3K3DES, OPT_AES);
        cbTypes.setValue(OPT_2K3DES);

        disableTextControl4Key();

        //configuration for table AppIDs
        appIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        selCol.setCellValueFactory(new PropertyValueFactory<>("type"));

        //configuration for table view History
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        cmdCol.setCellValueFactory(new PropertyValueFactory<>("command"));
        uidCol.setCellValueFactory(new PropertyValueFactory<>("uid"));
        receivedDataCol.setCellValueFactory(new PropertyValueFactory<>("receivedData"));
        tbViewHistory.setItems(historyData);

        setupUI();
    }

    @FXML
    private void onBtnAuthClicked(ActionEvent event) {
        lbResultTabAuth.setText("");
        String auth_key = null;
        boolean use_auth_KeyDiv = checkUseKeyDiv();
        try {
            SmartcardIoTransmitter transmitter = SmartcardIoTransmitter.create();
            ApduSession session = new ApduSession();
            StatusResponse status = null;
            String receivedData = "";

            if (use_auth_KeyDiv) {
                session.nextCommands(new GetVersion());
                status = session.transmit(transmitter);
            }
            auth_key = use_auth_KeyDiv
                    ? edDiversificationKey.getText()
                    : (edNoDivKey.isDisable()?"":edNoDivKey.getText());
            String uid = "";
            if (!use_auth_KeyDiv || (use_auth_KeyDiv && status.getStatus() == ApduStatus.SUCCESS)) {
                uid = getParamValueByKey(session, DesfireGetCardUID.CARD_UID_KEY);
                DesfireAuth authCmd = Utils.buildAuthPiccCmd(use_auth_KeyDiv,
                        cbTypes.getValue().equals("AES"), uid, auth_key);
                session.nextCommands(authCmd);
                status = session.transmit(transmitter);
                if (status.getStatus() == ApduStatus.SUCCESS) {
                    receivedData = "SessionKey: " + getParamValueByKey(session, DesfireAuth.SESSION_KEY);
                }
                btChangePiccKey.setVisible(status.getStatus() == ApduStatus.SUCCESS);
            }

            historyData.add(0, new HistoryModel(
                    status.getStatus().name(),//status
                    "DesfireAuth",//action
                    uid,//uid
                    receivedData));//received data

        } catch (CardException ex) {
            lbResultTabAuth.setText(ex.getMessage());
        } catch (RuntimeException ex2) {
            lbResultTabAuth.setText(ex2.getMessage());
        } catch (Exception ex3) {
            lbResultTabAuth.setText(ex3.getMessage());
        }
    }

    static public String getParamValueByKey(ApduSession session, String key) {
        if (session.getParameter(key).isPresent()) {
            return session.getParameter(key).get().getValue();
        }
        return "";
    }

    @FXML
    private void onBtnGetAppIDsClicked(ActionEvent event) {
        tbViewAppIDs.setItems(null);
        lbResultTabApp.setText("");
        try {
            SmartcardIoTransmitter transmitter = SmartcardIoTransmitter.create();
            ApduSession session = new ApduSession();
            StatusResponse status = null;

            String receivedData = ""; String key = null;
            boolean useKeyDiv = checkUseKeyDiv();
            if (useKeyDiv) {
                session.nextCommands(new GetVersion());
                status = session.transmit(transmitter);
            }
            key = useKeyDiv ? edDiversificationKey.getText().trim()
                    : edNoDivKey.getText().trim();

            String uid = "";
            if (!useKeyDiv || (useKeyDiv && status.getStatus() == ApduStatus.SUCCESS)) {
                uid = getParamValueByKey(session, DesfireGetCardUID.CARD_UID_KEY);
                DesfireAuth cmdDesfireAuth = Utils.buildAuthPiccCmd(
                        useKeyDiv, cbTypes.getValue().equals("AES"), uid, key);
                GetApplicationIds gai = new GetApplicationIds();
                session.nextCommands(
                        cmdDesfireAuth,
                        gai);
                status = session.transmit(transmitter);
                if (status.getStatus() == ApduStatus.SUCCESS) {
                    final ObservableList<ApplicationModel> data = FXCollections.observableArrayList(
                        new ApplicationModel("000000","NO")
                    );
                    receivedData = "000000;";
                    for (String id : gai.getApplicationIds()) {
                        data.add(new ApplicationModel(id,"NO"));
                        receivedData += id + ";";
                    }
                    tbViewAppIDs.setItems(data);
                } else {
                    lbResultTabApp.setText(uid + " ; DesfireAuth|GetApplicationIds - " + status.getStatus());
                }

            } else {
                lbResultTabApp.setText("GetVersion - " + status.getStatus());
            }

            historyData.add(0, new HistoryModel(
                status.getStatus().name(),//status
                "GetApplicationIds",//action
                uid,//uid
                receivedData));//received data

        } catch (CardException ex) {
            lbResultTabApp.setText(ex.getMessage());
            Logger.getLogger(TsCardExplorerController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RuntimeException ex2) {
            lbResultTabApp.setText(ex2.getMessage());
        }

    }

    @FXML
    private void onBtnSelectAppClicked(ActionEvent event) {
        StatusResponse status = null;
        try {
            SmartcardIoTransmitter transmitter = SmartcardIoTransmitter.create();
            ApduSession session = new ApduSession();
            Object selectedItem = tbViewAppIDs.getSelectionModel().getSelectedItem();
            SelectApplication selectAppCmd = new SelectTsApplication();
            String aid = "";
            if (selectedItem != null) {
                aid = ((ApplicationModel)selectedItem).getId();
                selectAppCmd = new SelectApplication(Hex.decode(aid));
            }
            session.nextCommands(
                    new GetVersion(),
                    selectAppCmd
            );
            status = session.transmit(transmitter);

            String uid = getParamValueByKey(session, DesfireGetCardUID.CARD_UID_KEY);
            String receivedData = "AID: " + getParamValueByKey(session, SelectTsApplication.AID_KEY);
            historyData.add(0, new HistoryModel(
                status.getStatus().name(),//status
                "SelectTsApplication",//action
                uid,//uid
                receivedData));//received data

            if (status.getStatus() == ApduStatus.SUCCESS) {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("fxml/AuthenticateApp.fxml"));
                Parent root = (Parent)fxmlLoader.load();
                AuthenticateAppController controller = fxmlLoader.<AuthenticateAppController>getController();
                controller.setSession(session, historyData);
                Stage stage = new Stage();
                stage.setTitle("Authenticate App - [" + aid + "]");
                stage.setScene(new Scene(root, 450, 300));
                stage.showAndWait();
            }

        } catch (Exception e) {
            lbResultTabApp.setText(e.getMessage());
        }

    }

    @FXML
    private void onBtnDeleteAppClicked(ActionEvent event) {
    }

    @FXML
    private void onRdDES(ActionEvent event) {
        disableTextControl4Key();
    }

    private void disableTextControl4Key() {
        edDiversificationKey.setDisable(!rbCmacAesKeyDiversification.isSelected());
        edNoDivKey.setDisable(!rbNoKeyDiversification.isSelected()
                || cbTypes.getValue() != "AES");
    }

    @FXML
    private void rdAes(ActionEvent event) {
        disableTextControl4Key();
    }

    @FXML
    private void onCBtextChanged(ActionEvent event) {
        setupUI();
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

    @FXML
    private void miClearOnAction(ActionEvent event) {
        historyData.clear();
    }

    @FXML
    private void onBtnCardInfoClicked(ActionEvent event) {
        try {
            SmartcardIoTransmitter transmitter = SmartcardIoTransmitter.create();
            ApduSession session = new ApduSession();
            StatusResponse status;

            String auth_key = null;
            if (checkUseKeyDiv()) {
                auth_key = edDiversificationKey.getText().trim();
            }
            String uid = "";
            session.nextCommands(new GetVersion());
            status = session.transmit(transmitter);
            uid = getParamValueByKey(session, DesfireGetCardUID.CARD_UID_KEY);

            session.nextCommands(Utils.buildAuthPiccCmd(
                    checkUseKeyDiv(), cbTypes.getValue().equals("AES"),
                    uid, auth_key));
            status = session.transmit(transmitter);

            session.nextCommands(new GetVersionExt()/*GetVersionExt*/);
            status = session.transmit(transmitter);

            String receivedData = "";
            if (status.getStatus() == ApduStatus.SUCCESS) {
                uid = getParamValueByKey(session, DesfireGetCardUID.CARD_UID_KEY);
                String prodBatchNumber = getParamValueByKey(session, GetVersionExt.PROD_BATCH_NUMBER);
                String calWeekOfProd = getParamValueByKey(session, GetVersionExt.CAL_WEEK_PROD);
                String yearOfProduction = getParamValueByKey(session, GetVersionExt.YEAR_PRODUCTION);
                receivedData = String.format(
                        "ProductionBatchNumber=%s; CalendarWeekOfProduction=%s; YearOfProduction=%s"
                        , prodBatchNumber, calWeekOfProd, yearOfProduction);

                historyData.add(0, new HistoryModel(
                    status.getStatus().name(),//status
                    "GetVersionExt",//action
                    uid,//uid
                    receivedData));//received data

                CardInfoModel cim = new CardInfoModel(
                        uid,
                        prodBatchNumber,
                        calWeekOfProd,
                        yearOfProduction,
                        getParamValueByKey(session, GetVersionExt.H_VENDOR_ID),//String hVendorId,
                        getParamValueByKey(session, GetVersionExt.H_TYPE),//String hType,
                        getParamValueByKey(session, GetVersionExt.H_SUBTYPE),//String hSubtype,
                        getParamValueByKey(session, GetVersionExt.H_MAJOR_VER),//String hMAJOR_VER,
                        getParamValueByKey(session, GetVersionExt.H_MINOR_VER),//String hMINOR_VER,
                        getParamValueByKey(session, GetVersionExt.H_STORAGE_SIZE),//String hStorageSize,
                        getParamValueByKey(session, GetVersionExt.H_COMM_PROTOCAL_TYPE),//String hCommProtocalType,
                        getParamValueByKey(session, GetVersionExt.S_VENDOR_ID),//String sVendorId,
                        getParamValueByKey(session, GetVersionExt.S_TYPE),//String sType,
                        getParamValueByKey(session, GetVersionExt.S_SUBTYPE),//String sSubtype,
                        getParamValueByKey(session, GetVersionExt.S_MINOR_VER),//String MajorVer,
                        getParamValueByKey(session, GetVersionExt.S_MINOR_VER),//String MinorVer,
                        getParamValueByKey(session, GetVersionExt.S_STORAGE_SIZE),//String sStorageSize,
                        getParamValueByKey(session, GetVersionExt.S_COMM_PROTOCAL_TYPE)//String sCommProtocalType
                        );
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("fxml/CardInfo.fxml"));
                Parent root = (Parent)fxmlLoader.load();
                CardInfoController controller = fxmlLoader.<CardInfoController>getController();
                controller.setCardInfoModel(cim);
                Stage stage = new Stage();
                stage.setTitle("Card Info");
                stage.setScene(new Scene(root, 650,430));

                controller.setStage(stage);

                stage.showAndWait();
            }
        } catch (Exception e) {
            lbResultTabApp.setText(e.getMessage());
        }
    }

    @FXML
    private void onBtnChangePiccKeyClicked(ActionEvent event) {
        try {
            SmartcardIoTransmitter transmitter = SmartcardIoTransmitter.create();
            ApduSession session = new ApduSession();
            session.nextCommands(new GetVersion());
            session.transmit(transmitter);

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("fxml/ChangePiccKey.fxml"));
            Parent root = (Parent)fxmlLoader.load();
            ChangePiccKeyController controller = fxmlLoader.<ChangePiccKeyController>getController();

            String curr_key = checkUseKeyDiv()
                ? edDiversificationKey.getText()
                : (edNoDivKey.isDisable()?"":edNoDivKey.getText());

            controller.setSession(session, historyData, checkUseKeyDiv(), curr_key,
                    cbTypes.getValue().equals("AES"));

            Stage stage = new Stage();
            stage.setTitle("Change PICC Key");
            stage.setScene(new Scene(root, 450, 300));
            stage.showAndWait();
        } catch (Exception e) {
            lbResultTabApp.setText(e.getMessage());
        }
    }

    private boolean checkUseKeyDiv() {
        return !rbCmacAesKeyDiversification.isDisable()
                && rbCmacAesKeyDiversification.isSelected();
    }

}
