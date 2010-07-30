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
package com.aimluck.eip.schedule;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.cayenne.access.DataContext;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALCellDateTimeField;
import com.aimluck.commons.field.ALCellStringField;
import com.aimluck.eip.cayenne.om.portlet.EipTScheduleMap;
import com.aimluck.eip.common.ALAbstractFormData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.orm.DatabaseOrmService;
import com.aimluck.eip.schedule.util.ScheduleUtils;
import com.aimluck.eip.services.eventlog.ALEventlogConstants;
import com.aimluck.eip.services.eventlog.ALEventlogFactoryService;
import com.aimluck.eip.util.ALEipUtils;

/**
 * スケジュールの状態変更を行うクラスです。
 *
 */
public class ScheduleChangeStatusFormData extends ALAbstractFormData {

  /** <code>logger</code> logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
      .getLogger(ScheduleChangeStatusFormData.class.getName());

  /** <code>status</code> 状態 */
  private ALCellStringField status;

  /** <code>tmpView</code> 表示する日 */
  private String tmpView;

  /** <code>end_date</code> 終了日時 */
  private ALCellDateTimeField view_date;

  public void loadParametersViewDate(RunData rundata, Context context) {
    if (ALEipUtils.isMatch(rundata, context)) {
      if (rundata.getParameters().containsKey("view_date")) {
        ALCellDateTimeField dummy = new ALCellDateTimeField("yyyy-MM-dd");
        tmpView = rundata.getParameters().getString("view_date");
        ALEipUtils.setTemp(rundata, context, "tmpView", tmpView);
        dummy.setValue(tmpView);
        if (!dummy.validate(new ArrayList())) {
          ALEipUtils.removeTemp(rundata, context, "tmpView");
          logger.debug("[ScheduleFormData] Parameter cannot validate");
          ALEipUtils.redirectPageNotFound(rundata);
          return;
        }
      }
    }
    tmpView = ALEipUtils.getTemp(rundata, context, "tmpView");
  }

  /*
   * @see com.aimluck.eip.common.ALData#initField()
   */
  public void initField() {
    // フィールドの初期化
    // 状態
    status = new ALCellStringField();
    status.setTrim(true);

    Date now = new Date();
    // 指定日時
    view_date = new ALCellDateTimeField("yyyy-MM-dd");
    if (tmpView == null || tmpView.equals("")) {
      view_date.setValue(now);
    } else {
      view_date.setValue(tmpView);
    }
    view_date.setFieldName("指定日時");
  }

  /*
   * @see com.aimluck.eip.common.ALAbstractFormData#setValidator()
   */
  protected void setValidator() {
    // Validateの定義必要なし
  }

  /*
   * @see
   * com.aimluck.eip.common.ALAbstractFormData#validate(java.util.ArrayList)
   */
  protected boolean validate(List<String> msgList) {
    // T: 仮スケジュール
    // C: 確認済みスケジュール
    // R: 棄却されたスケジュール
    // O: 通常スケジュール（オーナー）
    // 以上の文字列以外は入力チェックNGとする。
    return "T".equals(status.getValue()) || "C".equals(status.getValue())
        || "R".equals(status.getValue()) || "O".equals(status.getValue());
  }

  /*
   * @see
   * com.aimluck.eip.common.ALAbstractFormData#loadFormData(org.apache.turbine
   * .util.RunData, org.apache.velocity.context.Context, java.util.ArrayList)
   */
  protected boolean loadFormData(RunData rundata, Context context,
      List<String> msgList) {
    // このメソッドは利用されません。
    return false;
  }

  /*
   * @see
   * com.aimluck.eip.common.ALAbstractFormData#insertFormData(org.apache.turbine
   * .util.RunData, org.apache.velocity.context.Context, java.util.ArrayList)
   */
  protected boolean insertFormData(RunData rundata, Context context,
      List<String> msgList) {
    // このメソッドは利用されません。
    return false;
  }

  /*
   * @see
   * com.aimluck.eip.common.ALAbstractFormData#updateFormData(org.apache.turbine
   * .util.RunData, org.apache.velocity.context.Context, java.util.ArrayList)
   */
  protected boolean updateFormData(RunData rundata, Context context,
      List<String> msgList) throws ALDBErrorException, ALPageNotFoundException {
    try {
      // String org_id = OrgORMappingMap.getInstance().getOrgId(rundata);
      // ORMappingEipTScheduleMap orm_map = ScheduleOrmUtils
      // .getORMappingEipTScheduleMap(org_id);
      // if (orm_map == null)
      // return false;
      DataContext dataContext = DatabaseOrmService.getInstance()
          .getDataContext();

      // オブジェクトモデルを取得
      EipTScheduleMap schedule = ScheduleUtils.getEipTScheduleMap(rundata,
          context);
      if (schedule == null) {
        return false;
      }
      schedule.setStatus(status.getValue());
      dataContext.commitChanges();

      // イベントログに保存
      ALEventlogFactoryService
          .getInstance()
          .getEventlogHandler()
          .log(schedule.getScheduleId(),
              ALEventlogConstants.PORTLET_TYPE_SCHEDULE,
              schedule.getEipTSchedule().getName());

      // orm_map.doUpdate(schedule);
    } catch (Exception e) {

      logger.error("[ScheduleChangeStatusFormData]", e);
      throw new ALDBErrorException();

    }
    return true;
  }

  /*
   * @see
   * com.aimluck.eip.common.ALAbstractFormData#deleteFormData(org.apache.turbine
   * .util.RunData, org.apache.velocity.context.Context, java.util.ArrayList)
   */
  protected boolean deleteFormData(RunData rundata, Context context,
      List<String> msgList) {
    // このメソッドは利用されません。
    return false;
  }

  public ALCellDateTimeField getViewDate() {
    return view_date;
  }

}
