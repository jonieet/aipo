/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2008 Aimluck,Inc.
 * http://aipostyle.com/
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.aimluck.eip.modules.screens;

import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.timecard.TimecardSettingsFormData;
import com.aimluck.eip.util.ALEipUtils;

/**
 * タイムカード集計を処理するクラスです。 <br />
 * 
 */
public class TimecardSettingsFormScreen extends ALVelocityScreen {
  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
      .getLogger(TimecardSettingsFormScreen.class.getName());

  /**
   * @see org.apache.turbine.modules.screens.RawScreen#doOutput(org.apache.turbine.util.RunData)
   */
  @Override
  protected void doOutput(RunData rundata, Context context) throws Exception {
    try {
      ALEipUtils.setTemp(rundata, context, ALEipConstants.ENTITY_ID, "1");

      TimecardSettingsFormData formData = new TimecardSettingsFormData();
      formData.initField();
      formData.doViewForm(this, rundata, context);
      setTemplate(rundata, context,
          "portlets/html/ja/ajax-timecardsettings-form.vm");

    } catch (Exception ex) {
      logger.error("[TimecardSettingsFormScreen] Exception.", ex);
      ALEipUtils.redirectDBError(rundata);
    }
  }

}
