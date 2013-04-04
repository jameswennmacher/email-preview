/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.portlet.emailpreview;

import java.util.Date;

/**
 * Exchange email messages.  Exchange messages have a change key that must be sent back on
 * update messages to allow the Exchange server to determine if the Email Message is still in the
 * same state or if another process modified it.
 *
 * @author James Wennmacher, jwennmacher@unicon.net
 */

public class ExchangeEmailMessage extends EmailMessage {

    private final String exchangeChangeKey;

    public ExchangeEmailMessage(int messageNumber, String uid, String exchangeChangeKey, String sender, String subject,
                        Date sentDate, boolean unread, boolean answered, boolean deleted,
                        boolean multipart, String contentType, EmailMessageContent content, String allRecipients) {
        super(messageNumber,uid,sender,subject,sentDate,unread,answered,deleted,multipart,contentType,content,allRecipients);
        this.exchangeChangeKey = exchangeChangeKey;
    }

    public String getExchangeChangeKey() {
        return exchangeChangeKey;
    }
}
