/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2015 Aimluck,Inc.
 * http://www.aipo.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aimluck.eip.modules.screens;

import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.gpdb.GpdbFormData;
import com.aimluck.eip.gpdb.util.GpdbUtils;
import com.aimluck.eip.util.ALEipUtils;

/**
 *
 */
public class GpdbFormScreen extends ALVelocityScreen {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(GpdbFormScreen.class.getName());

  /**
   * 出力する
   * 
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @throws Exception
   *           例外
   */
  @Override
  protected void doOutput(RunData rundata, Context context) throws Exception {
    try {
      doGpdb_form(rundata, context);
    } catch (Exception ex) {
      logger.error("[GpdbFormScreen] Exception.", ex);
      ALEipUtils.redirectDBError(rundata);
    }
  }

  /**
   * 登録・編集画面用入力フォーム表示
   * 
   * @param rundata
   *          RunData
   * @param context
   *          Context
   */
  protected void doGpdb_form(RunData rundata, Context context) {
    GpdbFormData formData = new GpdbFormData();
    formData.initField();
    formData.doViewForm(this, rundata, context);

    String layout_template = "portlets/html/ja/ajax-gpdb-form.vm";
    setTemplate(rundata, context, layout_template);
  }

  /**
   * ポートレット名取得
   * 
   * @return ポートレット名
   */
  @Override
  protected String getPortletName() {
    return GpdbUtils.GPDB_PORTLET_NAME;
  }

}
