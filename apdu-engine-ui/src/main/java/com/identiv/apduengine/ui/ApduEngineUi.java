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
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.identiv.apduengine.ui;

import com.identiv.apduengine.ApduSession;
import com.identiv.apduengine.engine.ApduTransmitter;
import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 */
public class ApduEngineUi extends Application {
    ApduTransmitter transmitter;
    ApduSession session = new ApduSession();

    @Override
    public void start(Stage primaryStage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("fxml/TsCardExplorer.fxml"));
        Scene scene = new Scene(root);

        primaryStage.setTitle("TS card explorer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     */
    public static void main(String[] args) {
        launch(args);
    }

}
