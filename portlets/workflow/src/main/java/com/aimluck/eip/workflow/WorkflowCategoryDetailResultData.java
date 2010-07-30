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
package com.aimluck.eip.workflow;

import java.util.StringTokenizer;

import com.aimluck.commons.field.ALStringField;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.workflow.util.WorkflowUtils;

/**
 * ワークフローカテゴリのResultDataです。<BR>
 *
 */
public class WorkflowCategoryDetailResultData extends
    WorkflowCategoryResultData {
  /** メモ */
  protected ALStringField note;

  /** テンプレート */
  protected ALStringField ordertemplate;

  /** 登録日 */
  protected ALStringField create_date;

  /** 更新日 */
  protected ALStringField update_date;

  /** 申請経路名 */
  private ALStringField route_name;

  /** 申請経路 */
  protected ALStringField route;

  /**
   *
   * @see com.aimluck.eip.common.ALData#initField()
   */
  public void initField() {
    super.initField();
    note = new ALStringField();
    ordertemplate = new ALStringField();
    create_date = new ALStringField();
    update_date = new ALStringField();
    route_name = new ALStringField();
    route = new ALStringField();
  }

  /**
   * @return
   */
  public String getNote() {
    return ALEipUtils.getMessageList(note.getValue());
  }

  /**
   * @return
   */
  public ALStringField getCreateDate() {
    return create_date;
  }

  /**
   * @return
   */
  public ALStringField getUpdateDate() {
    return update_date;
  }

  /**
   * @return
   */
  public String getTemplate() {
    return ALEipUtils.getMessageList(ordertemplate.getValue());
  }

  /**
   * @param string
   */
  public void setTemplate(String string) {
    ordertemplate.setValue(string);
  }

  /**
   * @param string
   */
  public void setNote(String string) {
    note.setValue(string);
  }

  /**
   * @param string
   */
  public void setCreateDate(String string) {
    create_date.setValue(string);
  }

  /**
   * @param string
   */
  public void setUpdateDate(String string) {
    update_date.setValue(string);
  }

  /**
   * @param string
   */
  public ALStringField getRouteName() {
    return route_name;
  }

  /**
   * @param string
   */
  public void setRouteName(String string) {
    route_name.setValue(string);
  }

  /**
   * @param string
   */
  public void setRoute(String string) {
    route.setValue(string);
  }

  /**
   * @param string
   */
  public ALStringField getRoute(String string) {
    return route;
  }

  public String getRouteH() {
    StringBuffer routeun = new StringBuffer();
    String username;

    if (route.getValue() != null && !"".equals(route.getValue())) {
      StringTokenizer st = new StringTokenizer(route.getValue(), ",");
      while (st.hasMoreTokens()) {
        username = WorkflowUtils.getName(st.nextToken());
        routeun.append(username);
        routeun.append(" -> ");
      }
      routeun.append("完了");

      return routeun.toString();
    }
    return "";
  }

  public boolean getHasRootName() {

    boolean bool;

    if (route.getValue() == null || route.getValue() == "") {
      bool = false;
    } else {
      bool = true;
    }

    return bool;
  }
}
