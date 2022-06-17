/*
 *  Copyright 2009-2020 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.bloomreach.forge.localization.shortcut;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.servlet.http.Cookie;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.settings.GlobalSettings;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.util.WebApplicationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LocalizationShortcutPlugin extends RenderPlugin<Object> implements IHeaderContributor {

    private static final Logger log = LoggerFactory.getLogger(LocalizationShortcutPlugin.class);

    private static final String PARAM_SHORTCUT_LINK_LABEL = "shortcut-link-label";

    private static final String DEFAULT_LANGUAGE = "en";
    private static final String LOCALES = "locales";
    public static final String[] DEFAULT_LOCALES = {"en"};

    private static final String PARAM_SHOW_TIMEZONES = "show.timezones";
    public static final String SELECTABLE_TIMEZONES_CONFIG_PARAM = "selectable.timezones";
    private List<String> availableTimeZones = Collections.emptyList();
    public static final List<String> SUPPORTED_JAVA_TIMEZONES = Collections.unmodifiableList(
            getSupportedJavaTimeZones());

    /**
     * Exclude POSIX compatible timezones because they may cause confusions
     */
    private static List<String> getSupportedJavaTimeZones() {
        return Arrays.stream(TimeZone.getAvailableIDs())
                .filter(tz -> !tz.startsWith("Etc/"))
                .collect(Collectors.toList());
    }


    public LocalizationShortcutPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        AjaxLink<Object> link = new LocalizationShortcutPlugin.Link("link", context, config, this);
        add(link);

        final IPluginConfig localeConfig = getLocalizedPluginConfig(config);
        Label labelComponent;

        if (localeConfig != null) {
            String labelText = localeConfig.getString(PARAM_SHORTCUT_LINK_LABEL,
                    "Warning: label not found: " + PARAM_SHORTCUT_LINK_LABEL);
            labelComponent = new Label(PARAM_SHORTCUT_LINK_LABEL, Model.of(labelText));
        } else {
            labelComponent = new Label(PARAM_SHORTCUT_LINK_LABEL,
                    new StringResourceModel(PARAM_SHORTCUT_LINK_LABEL, this));
        }

        link.add(labelComponent);

        final HippoIcon icon = HippoIcon.fromSprite("shortcut-icon", Icon.GLOBE);
        link.add(icon);
    }

    private IPluginConfig getLocalizedPluginConfig(final IPluginConfig config) {
        Locale locale = getSession().getLocale();
        String localeString = getSession().getLocale().toString();
        IPluginConfig localeConfig = config.getPluginConfig(localeString);

        // just in case the locale contains others than language code, try to find it by language code again
        if (localeConfig == null && !StringUtils.equals(locale.getLanguage(), localeString)) {
            localeConfig = config.getPluginConfig(locale.getLanguage());
        }

        // if still not found, then try to find it by the default language again.
        if (localeConfig == null && !StringUtils.equals(DEFAULT_LANGUAGE, locale.getLanguage())) {
            localeConfig = config.getPluginConfig(DEFAULT_LANGUAGE);
        }

        return localeConfig;
    }

    /**
     * The link that opens a dialog window.
     */
    private class Link extends AjaxLink<Object> {

        private final IPluginContext context;
        private final IPluginConfig config;
        private final LocalizationShortcutPlugin parent;

        Link(final String id, final IPluginContext context, final IPluginConfig config, LocalizationShortcutPlugin parent) {
            super(id);
            this.context = context;
            this.config = config;
            this.parent = parent;
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            parent.getDialogService().show(getDialog(context, config, parent));
        }
    }

    protected LocalizationShortcutPlugin.Dialog getDialog(final IPluginContext context, final IPluginConfig config, LocalizationShortcutPlugin parent) {
        return new LocalizationShortcutPlugin.Dialog(context, config, parent);
    }

    /**
     * The dialog that opens after the user has clicked the dashboard link.
     */
    protected class Dialog extends org.hippoecm.frontend.dialog.Dialog<Object> {

        protected static final String DIALOG_LOCALE_LABEL = "locale-label";
        protected static final String DIALOG_TIMEZONE_LABEL = "timezone-label";

        private final IPluginContext context;
        private final IPluginConfig config;

        protected final DropDownChoice<String> locale;
        protected final DropDownChoice<String> timezone;
        protected String selectedTimezone;
        protected Boolean isTimeZoneVisible;
        protected String selectedLocale;

        private static final String LOCALE_COOKIE = "loc";
        private static final String TZ_COOKIE = "tzcookie";
        private static final int COOKIE_MAX_AGE = 365 * 24 * 3600; // expire one year from now

        /**
         * @param context plugin context
         * @param config  plugin config
         * @param parent  parent component (no longer used)
         */
        public Dialog(final IPluginContext context, final IPluginConfig config, @SuppressWarnings("unused") Component parent) {
            this.context = context;
            this.config = config;

            setSize(DialogConstants.MEDIUM_AUTO);

            // get values from the configuration

            isTimeZoneVisible = config.getAsBoolean(PARAM_SHOW_TIMEZONES);

            // build UI
            feedback = new FeedbackPanel("feedback");
            replace(feedback);
            feedback.setOutputMarkupId(true);

            // get locales from cms settings
            String[] localeArray = GlobalSettings.get().getStringArray(LOCALES);
            if (localeArray == null || localeArray.length == 0) {
                localeArray = DEFAULT_LOCALES;
            }

            // get timezones list from plugin property or fallback on list of supported timezones
            if (isTimeZoneVisible) {
                final String[] configuredTimezones = config.getStringArray(SELECTABLE_TIMEZONES_CONFIG_PARAM);
                availableTimeZones = getSelectableTimezones(configuredTimezones);

                // Check if user has previously selected a timezone
                final String cookieTimeZone = getCookieValue(TZ_COOKIE);
                if (isTimeZoneValid(cookieTimeZone)) {
                    selectedTimezone = cookieTimeZone;
                }
            }



            // add list dropdown field
            Label listLabel = getLabel(DIALOG_LOCALE_LABEL, config);
            add(listLabel);

            final String defaultLocale = DEFAULT_LOCALE;
            initSelectedLocale(Arrays.asList(localeArray), defaultLocale);
            getSession().setLocale(getSelectedLocale());
            final List <String> locales = Arrays.asList(localeArray);

            locale = new DropDownChoice<>("locale",
                    new PropertyModel<String>(this, "selectedLocale") {
                        @Override
                        public void setObject(final String object) {
                            super.setObject(locales.contains(object) ? object : defaultLocale);
                        }
                    },
                   locales,
                    // Display the language name from i18n properties
                    new IChoiceRenderer<String>() {
                        public String getDisplayValue(final String key) {
                            final Locale localeFromKey = new Locale(key);
                            return StringUtils.capitalize(localeFromKey.getDisplayLanguage(localeFromKey));
                        }

                        public String getIdValue(final String object, final int index) {
                            return object;
                        }

                        @Override
                        public String getObject(final String id, final IModel<? extends List<? extends String>> choicesModel) {
                            final List<? extends String> choices = choicesModel.getObject();
                            return choices.contains(id) ? id : null;
                        }
                    }
            );
            add(locale);

            // add timezones dropdown field
            final Label timezoneLabel = getLabel(DIALOG_TIMEZONE_LABEL, config);
            add(timezoneLabel);
            timezone = new DropDownChoice<>("timezone",
                    new PropertyModel<String>(this, "selectedTimezone") {
                        @Override
                        public void setObject(final String object) {
                            super.setObject(availableTimeZones.contains(object) ? object : availableTimeZones.get(0) );
                        }
                    },
                    availableTimeZones
            );

            add(timezone);
            if (!isTimeZoneVisible) {
                timezoneLabel.setVisible(false);
                timezone.setVisible(false);
            }

        }

        /**
         * Get a label from the plugin config, or from the Dialog properties file.
         *
         * @param labelKey the key under which the label is stored
         * @param config   the config of the plugin
         * @return a wicket Label
         */
        protected Label getLabel(final String labelKey, final IPluginConfig config) {
            final IPluginConfig localeConfig = getLocalizedPluginConfig(config);
            if (localeConfig != null) {
                final String label = localeConfig.getString(labelKey);
                if (StringUtils.isNotBlank(label)) {
                    return new Label(labelKey, label);
                }
            }
            return new Label(labelKey, new StringResourceModel(labelKey, this).setModel(null).setParameters(
            ));
        }

        /**
         * Gets the dialog title from the config.
         *
         * @return the label, or a warning if not found.
         */
        @Override
        public IModel<String> getTitle() {
            final IPluginConfig localeConfig = getLocalizedPluginConfig(config);
            if (localeConfig != null) {
                String label = localeConfig.getString(PARAM_SHORTCUT_LINK_LABEL);
                if (StringUtils.isNotBlank(label)) {
                    return Model.of(label);
                }
            }
            return new StringResourceModel(PARAM_SHORTCUT_LINK_LABEL, this);
        }

        @Override
        protected void onOk() {
            try {
                log.debug("Setting locale to: {}", selectedLocale);

                // Store locale in cookie
                setCookieValue(LOCALE_COOKIE, selectedLocale, COOKIE_MAX_AGE);
                // and update the session locale
                getSession().setLocale(Locale.forLanguageTag(selectedLocale));

                if (isTimeZoneVisible){
                    if(selectedTimezone != null) {
                        log.debug("Setting timezone to: {}", selectedTimezone);
                        setCookieValue(TZ_COOKIE, selectedTimezone, COOKIE_MAX_AGE);
                        setTimeZone(selectedTimezone);
                    } else log.debug("Timezone not set!");
                }
                //trigger a full page reload (incl. navapp)
                RequestCycle.get().find(AjaxRequestTarget.class).ifPresent(target -> target.appendJavaScript("window.top.location.reload();"));
            } catch (Exception e) {
                log.error(e.getClass().getSimpleName() + " occurred while changing language setting", e);
            }
        }

        protected void setCookieValue(final String cookieName, final String cookieValue, final int maxAge) {
            final Cookie localeCookie = new Cookie(cookieName, cookieValue);
            localeCookie.setMaxAge(maxAge);
            localeCookie.setHttpOnly(true);
            WebApplicationHelper.retrieveWebResponse().addCookie(localeCookie);
        }

        protected String getCookieValue(final String cookieName) {
            final Cookie[] cookies = WebApplicationHelper.retrieveWebRequest().getContainerRequest().getCookies();
            if (cookies != null) {
                for (final Cookie cookie : cookies) {
                    if (cookieName.equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
            return null;
        }

        private void initSelectedLocale(final List<String> locales, final String defaultLocale) {
            final String cookieLocale = getCookieValue(LOCALE_COOKIE);
            final String sessionLocale = getSession().getLocale().getLanguage();
            if (cookieLocale != null && locales.contains(cookieLocale)) {
                selectedLocale = cookieLocale;
            } else if (sessionLocale != null && locales.contains(sessionLocale)) {
                selectedLocale = sessionLocale;
            } else {
                selectedLocale = defaultLocale;
            }

        }

        private Locale getSelectedLocale() {
            if (selectedLocale.equals(Locale.CHINESE.getLanguage())) {
                // always use simplified Chinese, Wicket does not known Chinese without a country
                return Locale.SIMPLIFIED_CHINESE;
            }
            return new Locale(selectedLocale);
        }

    }

    protected void setTimeZone(String selectedTimeZone) {
        if (isTimeZoneValid(selectedTimeZone)) {
            final TimeZone timeZone = TimeZone.getTimeZone(selectedTimeZone);
            // Store selected timezone in session and cookie
            UserSession.get().getClientInfo().getProperties().setTimeZone(timeZone);
        }
    }

    private boolean isTimeZoneValid(final String timeZone) {
        return timeZone != null && availableTimeZones != null
                && availableTimeZones.contains(timeZone);
    }

    private List<String> getSelectableTimezones(final String[] configuredSelectableTimezones) {
        List<String> selectableTimezones = new ArrayList<>();

        if (configuredSelectableTimezones != null) {
            selectableTimezones = Arrays.stream(configuredSelectableTimezones)
                    .filter(StringUtils::isNotBlank)
                    .filter(SUPPORTED_JAVA_TIMEZONES::contains)
                    .collect(Collectors.toList());
        }

        return selectableTimezones.isEmpty() ? SUPPORTED_JAVA_TIMEZONES : selectableTimezones;
    }
}
