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
package org.jasig.portlet.emailpreview.controller;

import java.util.HashMap;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletMode;
import javax.portlet.PortletModeException;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.portlet.emailpreview.MailStoreConfiguration;
import org.jasig.portlet.emailpreview.dao.IEmailAccountDao;
import org.jasig.portlet.emailpreview.dao.IMailStoreDao;
import org.jasig.portlet.emailpreview.dao.MailPreferences;
import org.jasig.portlet.emailpreview.mvc.Attribute;
import org.jasig.portlet.emailpreview.mvc.MailStoreConfigurationForm;
import org.jasig.portlet.emailpreview.service.auth.IAuthenticationService;
import org.jasig.portlet.emailpreview.service.auth.IAuthenticationServiceRegistry;
import org.jasig.portlet.emailpreview.service.auth.PortletPreferencesCredentialsAuthenticationServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.portlet.ModelAndView;

@Controller
@RequestMapping("EDIT")
public final class EditPreferencesController {
    
    private static final String UNCHANGED_PASSWORD = "uNch@ng3d.pswd!";
    private static final String CONFIG_FORM_KEY = "org.jasig.portlet.emailpreview.controller.CONFIG_FORM_KEY";

    private IMailStoreDao mailStoreDao;
    IEmailAccountDao emailAccountDao;
    private IAuthenticationServiceRegistry authServiceRegistry;
    private final Log log = LogFactory.getLog(getClass());

    @RequestMapping
    public ModelAndView getAccountFormView(RenderRequest req) {

        Map<String,Object> model = new HashMap<String,Object>();
        MailStoreConfiguration config = mailStoreDao.getConfiguration(req);
        
        // form
        PortletSession session = req.getPortletSession(false);
        MailStoreConfigurationForm form = (MailStoreConfigurationForm) session.getAttribute(CONFIG_FORM_KEY);
        if (form == null) {
            form = MailStoreConfigurationForm.create(mailStoreDao, req);
        } else {
            session.removeAttribute(CONFIG_FORM_KEY);
        }
        model.put("form", form);
        
        // Disable some config elements?
        model.put("disableProtocol", mailStoreDao.isReadOnly(req, MailPreferences.PROTOCOL));
        model.put("disableHost", mailStoreDao.isReadOnly(req, MailPreferences.HOST));
        model.put("disablePort", mailStoreDao.isReadOnly(req, MailPreferences.PORT));
        model.put("disableAuthService", mailStoreDao.isReadOnly(req, MailPreferences.AUTHENTICATION_SERVICE_KEY));

        // AuthN info
        Map<String,IAuthenticationService> authServices = new HashMap<String,IAuthenticationService>();
        for (String key : config.getAllowableAuthenticationServiceKeys()) {
            IAuthenticationService auth = authServiceRegistry.getAuthenticationService(key);
            if (auth != null) {
                authServices.put(key, auth);
            } else {
                // Unknown authN service;  bad data
                if (log.isWarnEnabled()) {
                    log.warn("Portlet specified an allowable Authentication " +
                            "Service that is unknown to the registry:  '" + 
                            key + "'");
                }
            }
        }
        model.put("authenticationServices", authServices);
        if (form.getAdditionalProperties().containsKey(MailPreferences.PASSWORD.getKey())) {
            model.put("unchangedPassword", UNCHANGED_PASSWORD);
        }
        
        // Pass the errorMessage, if present
        if (req.getParameter("errorMessage") != null) {
            model.put("errorMessage", req.getParameter("errorMessage"));
        }
        
        return new ModelAndView("editPreferences", model);

    }

    @RequestMapping(params = "action=updatePreferences")
    public void updatePreferences(ActionRequest req, ActionResponse res) throws PortletModeException {
        
        MailStoreConfiguration config = mailStoreDao.getConfiguration(req);
        MailStoreConfigurationForm form = MailStoreConfigurationForm.create(mailStoreDao, req);
        String err = null;  // default
        String mailAccountAtStart = config.getAdditionalProperties().get(MailPreferences.MAIL_ACCOUNT.getKey());

        if (!mailStoreDao.isReadOnly(req, MailPreferences.PROTOCOL)) {
            String protocol = req.getParameter(MailPreferences.PROTOCOL.getKey());
            protocol = protocol != null ? protocol.trim() : "";
            if (log.isDebugEnabled()) {
                log.debug("Receieved the following user input for Protocol:  '" + protocol + "'");
            }
            form.setProtocol(protocol);
            if (protocol.length() == 0 && err == null) {
                err = "Server Protocol is required";
            }
        }
        
        if (!mailStoreDao.isReadOnly(req, MailPreferences.HOST)) {
            String host = req.getParameter(MailPreferences.HOST.getKey());
            host = host != null ? host.trim() : "";
            if (log.isDebugEnabled()) {
                log.debug("Receieved the following user input for Host:  '" + host + "'");
            }
            form.setHost(host);
            if (host.length() == 0 && err == null) {
                err = "Server Name is required";
            }
        }

        if (!mailStoreDao.isReadOnly(req, MailPreferences.PORT)) {
            String port = req.getParameter(MailPreferences.PORT.getKey());
            port = port != null ? port.trim() : "";
            if (log.isDebugEnabled()) {
                log.debug("Receieved the following user input for Port:  '" + port + "'");
            }
            try {
                form.setPort(Integer.parseInt(port));
            } catch (NumberFormatException nfe) {
                log.debug(nfe);
            }
            if (port.length() == 0 && err == null) {
                err = "Server Port is required";
            }
        }
        
        if (!mailStoreDao.isReadOnly(req, MailPreferences.AUTHENTICATION_SERVICE_KEY)) {
            String authKey = req.getParameter(MailPreferences.AUTHENTICATION_SERVICE_KEY.getKey());
            authKey = authKey != null ? authKey.trim() : "";
            if (log.isDebugEnabled()) {
                log.debug("Receieved the following user input for AuthN Service Key:  '" + authKey + "'");
            }
            if (authKey.length() != 0 && config.getAllowableAuthenticationServiceKeys().contains(authKey)) {  // authKey radio buttons may not be present
                form.setAuthenticationServiceKey(authKey);
            }
        }

        // ToDo:  Support for PortletPreferences auth is a 
        // bit hackish;  look for an opportunity to refactor 
        // toward abstractions.
        String ppPassword = null;  // default
        if (PortletPreferencesCredentialsAuthenticationServiceImpl.KEY.equals(form.getAuthenticationServiceKey())) {
            
            // Update username
            if (!mailStoreDao.isReadOnly(req, MailPreferences.MAIL_ACCOUNT)) {
                String mailAccount = req.getParameter(MailPreferences.MAIL_ACCOUNT.getKey());
                mailAccount = mailAccount != null ? mailAccount.trim() : "";
                if (log.isDebugEnabled()) {
                    log.debug("Receieved the following user input for mailAccount:  '" + mailAccount + "'");
                }
                if (mailAccount.length() != 0) {
                    form.getAdditionalProperties().put(MailPreferences.MAIL_ACCOUNT.getKey(), new Attribute(mailAccount));
                } else {
                    form.getAdditionalProperties().remove(MailPreferences.MAIL_ACCOUNT.getKey());
                }
            }

            // Update password, if entered & confirmed
            if (!mailStoreDao.isReadOnly(req, MailPreferences.PASSWORD)) {
                String password = req.getParameter("ppauth_password");
                password = password != null ? password.trim() : "";
                if (log.isDebugEnabled()) {
                    log.debug("Receieved user input of the following length for Password:  " + password.length());
                }
                if (!UNCHANGED_PASSWORD.equals(password)) {
                    if (password.length() > 0) {
                        String confirm = req.getParameter("ppauth_confirm");
                        confirm = confirm != null ? confirm.trim() : "";
                        if (log.isDebugEnabled()) {
                            log.debug("Receieved user input of the following length for Confirm:  " + confirm.length());
                        }
                        if (confirm.equals(password)) {
                            // Don't put the password back into the form!
                            ppPassword = password;
                        } else {
                            err = "Password and Confirm Password fields must match";
                            form.getAdditionalProperties().remove(MailPreferences.PASSWORD.getKey());
                        }
                    } else {
                        err = "Password is required for this form of authentication";
                        form.getAdditionalProperties().remove(MailPreferences.PASSWORD.getKey());
                    }
                }
            }

        }
        
        // Proceed if there were no problems
        if (err == null) {

            // protocol/host/port
            config.setProtocol(form.getProtocol());
            config.setHost(form.getHost());
            config.setPort(form.getPort());
            config.setAuthenticationServiceKey(form.getAuthenticationServiceKey());
            
            // username/password
            if (PortletPreferencesCredentialsAuthenticationServiceImpl.KEY.equals(form.getAuthenticationServiceKey())) {
                Attribute username = form.getAdditionalProperties().get(MailPreferences.MAIL_ACCOUNT.getKey());
                config.getAdditionalProperties().put(MailPreferences.MAIL_ACCOUNT.getKey(), username.getValue());
                // NB:  we only accept password if username was also specified
                if (ppPassword != null) {
                    config.getAdditionalProperties().put(MailPreferences.PASSWORD.getKey(), ppPassword);
                }
            } else {
                // Make sure username/password are clear
                config.getAdditionalProperties().remove(MailPreferences.MAIL_ACCOUNT.getKey());
                config.getAdditionalProperties().remove(MailPreferences.PASSWORD.getKey());
            }
            
            mailStoreDao.saveConfiguration(req, config);
            emailAccountDao.clearCache(req.getRemoteUser(), mailAccountAtStart);
            res.setPortletMode(PortletMode.VIEW);
        } else {
            res.setRenderParameter("errorMessage", err);
            req.getPortletSession().setAttribute(CONFIG_FORM_KEY, form);
        }
        
    }

    @Autowired(required = true)
    public void setAuthenticationServiceRegistry(IAuthenticationServiceRegistry authServiceRegistry) {
        this.authServiceRegistry = authServiceRegistry;
    }
    
    @Autowired(required = true)
    public void setEmailAccountDao(IEmailAccountDao emailAccountDao) {
        this.emailAccountDao = emailAccountDao;
    }

    @Autowired(required = true)
    public void setMailStoreDao(IMailStoreDao mailStoreDao) {
        this.mailStoreDao = mailStoreDao;
    }

}