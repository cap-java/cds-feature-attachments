/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.configuration;

import com.sap.cds.services.messages.LocalizedMessageProvider;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link LocalizedMessageProvider} that resolves error messages from a package-qualified resource
 * bundle ({@code com.sap.cds.feature.attachments.i18n.errors}), avoiding classpath conflicts with
 * the consuming application's {@code messages.properties}.
 *
 * <p>Resolution order:
 *
 * <ol>
 *   <li>Look up {@code messageOrKey} in the plugin's own resource bundle
 *   <li>If not found, delegate to the previous provider in the chain
 * </ol>
 */
public class AttachmentsLocalizedMessageProvider implements LocalizedMessageProvider {

  private static final Logger logger =
      LoggerFactory.getLogger(AttachmentsLocalizedMessageProvider.class);

  static final String BUNDLE_NAME = "com.sap.cds.feature.attachments.i18n.errors";

  private LocalizedMessageProvider previous;

  @Override
  public void setPrevious(LocalizedMessageProvider previous) {
    this.previous = previous;
  }

  @Override
  public String get(String messageOrKey, Object[] args, Locale locale) {
    String resolved = resolveFromBundle(messageOrKey, args, locale);
    if (resolved != null) {
      return resolved;
    }
    if (previous != null) {
      return previous.get(messageOrKey, args, locale);
    }
    return null;
  }

  private static String resolveFromBundle(String key, Object[] args, Locale locale) {
    try {
      Locale effectiveLocale = locale != null ? locale : Locale.getDefault();
      ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME, effectiveLocale);
      if (bundle.containsKey(key)) {
        String pattern = bundle.getString(key);
        return new MessageFormat(pattern, effectiveLocale)
            .format(args != null ? args : new Object[0]);
      }
    } catch (MissingResourceException e) {
      logger.debug("Resource bundle '{}' not found for key '{}'", BUNDLE_NAME, key, e);
    }
    return null;
  }
}
