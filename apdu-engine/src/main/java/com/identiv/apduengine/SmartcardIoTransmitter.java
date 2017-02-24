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
package com.identiv.apduengine;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.identiv.apduengine.engine.ApduResponse;
import com.identiv.apduengine.engine.ApduTransmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;
import java.nio.ByteBuffer;

/**
 */
public class SmartcardIoTransmitter implements ApduTransmitter {

    static Logger logger = LoggerFactory.getLogger(SmartcardIoTransmitter.class);

    Card card;

    public SmartcardIoTransmitter() {}

    public SmartcardIoTransmitter(Card card) {
        this.card = card;
    }

    public void setCard(Card card) {
        this.card = card;
    }

    @Override
    public ListenableFuture<ApduResponse> apply(byte[] in) {
        // how to actually transmit to the card
        // here we just use the normal transmit stuff
        try {
            ByteBuffer command = ByteBuffer.wrap(in);
            ByteBuffer response = ByteBuffer.allocate(1024); // TODO is this reasonable?
            int size = card.getBasicChannel().transmit(command, response);

            byte[] out = new byte[size];
            System.arraycopy(response.array(), 0,
                    out, 0, size);

            return Futures.immediateFuture(new ApduResponse(out));
        } catch (CardException e) {
            throw new RuntimeException(e);
        }
    }


    public static SmartcardIoTransmitter create() throws CardException {
        TerminalFactory tf = TerminalFactory.getDefault();

        CardTerminal cardTerminal = null;

        if (tf.terminals().list().size() == 1) {
            cardTerminal = tf.terminals().list().get(0);
        } else {
            for (CardTerminal terminal : tf.terminals().list()) {
                logger.info("Checking for card: {}", terminal.getName());
                if (terminal.isCardPresent()) {
                    cardTerminal = terminal;
                }
            }
        }

        if (cardTerminal != null) {
            cardTerminal.waitForCardPresent(0);
        } else {
            throw new RuntimeException("No card found!");
        }

        logger.info("Using -> " + cardTerminal.getName());

        Card card = cardTerminal.connect("*");

        return new SmartcardIoTransmitter(card);

    }

}
