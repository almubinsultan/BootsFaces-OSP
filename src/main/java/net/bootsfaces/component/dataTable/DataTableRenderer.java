/**
 *  Copyright 2014-2017 Riccardo Massera (TheCoder4.Eu) and Stephan Rauh (http://www.beyondjava.net).
 *
 *  This file is part of BootsFaces.
 *
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
 */

package net.bootsfaces.component.dataTable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.FacesRenderer;
import net.bootsfaces.C;
import net.bootsfaces.component.ajax.AJAXRenderer;
import net.bootsfaces.component.dataTableColumn.DataTableColumn;
import net.bootsfaces.render.CoreRenderer;
import net.bootsfaces.render.Responsive;
import net.bootsfaces.render.Tooltip;
import net.bootsfaces.utils.BsfUtils;

/** This class generates the HTML code of &lt;b:dataTable /&gt;. */
@FacesRenderer(componentFamily = "net.bootsfaces.component", rendererType = "net.bootsfaces.component.dataTable.DataTable")
public class DataTableRenderer extends CoreRenderer {

	@Override
	public void decode(FacesContext context, UIComponent component) {
		DataTable dataTable = (DataTable) component;
		if (dataTable.isDisabled()) {
			return;
		}

		decodeBehaviors(context, dataTable); // f:ajax

		String clientId = dataTable.getClientId(context);
		new AJAXRenderer().decode(context, component, clientId);
	}

	private static final Pattern NUMERIC_PATTERN = Pattern.compile("[0-9]+");

	/**
	 * This methods generates the HTML code of the current b:dataTable.
	 * <code>encodeBegin</code> generates the start of the component. After the, the
	 * JSF framework calls <code>encodeChildren()</code> to generate the HTML code
	 * between the beginning and the end of the component. For instance, in the case
	 * of a panel component the content of the panel is generated by
	 * <code>encodeChildren()</code>. After that, <code>encodeEnd()</code> is called
	 * to generate the rest of the HTML code.
	 *
	 * @param context   the FacesContext.
	 * @param component the current b:dataTable.
	 * @throws IOException thrown if something goes wrong when writing the HTML
	 *                     code.
	 */
	@Override
	public void encodeBegin(FacesContext context, UIComponent component) throws IOException {

		if (!component.isRendered()) {
			return;
		}
		DataTable dataTable = (DataTable) component;

		ResponseWriter rw = context.getResponseWriter();
		String clientId = dataTable.getClientId();
		boolean idHasBeenRendered = false;

		if (dataTable.isScrollHorizontally()) {
			rw.startElement("div", dataTable);
			rw.writeAttribute("class", "table-responsive", null);
			rw.writeAttribute("id", clientId, "id");
			idHasBeenRendered = true;
		}

		String responsiveStyle = Responsive.getResponsiveStyleClass(dataTable, false);
		if (null != responsiveStyle && responsiveStyle.trim().length() > 0) {
			rw.startElement("div", dataTable);
			rw.writeAttribute("class", responsiveStyle.trim(), null);
			if (!idHasBeenRendered) {
				rw.writeAttribute("id", clientId, "id");
				idHasBeenRendered = true;
			}
		}

		if (dataTable.isContentDisabled()) {
			if (beginDisabledFieldset(dataTable, rw)) {
				rw.writeAttribute("id", clientId, "id");
				idHasBeenRendered = true;
			}
		}

		rw.startElement("table", dataTable);
		if (!idHasBeenRendered) {
			rw.writeAttribute("id", clientId, "id");
		} else
			rw.writeAttribute("id", clientId + "Inner", "id"); // Table selection needs a valid AJAX id on the table tag

		String styleClass = "table ";
		if (dataTable.isBorder()) {
			styleClass += "table-bordered ";
		}
		if (dataTable.isStriped()) {
			styleClass += "table-striped ";
		}
		if (dataTable.isRowHighlight())
			styleClass += "table-hover ";
		if (dataTable.getStyleClass() != null)
			styleClass += dataTable.getStyleClass();
		styleClass += " " + clientId.replace(":", "") + "Table";
		rw.writeAttribute("class", styleClass, "class");
		Tooltip.generateTooltip(context, dataTable, rw);
		rw.writeAttribute("cellspacing", "0", "cellspacing");
		rw.writeAttribute("style", dataTable.getStyle(), "style");
		AJAXRenderer.generateBootsFacesAJAXAndJavaScript(context, dataTable, rw, false);

		generateCaption(context, dataTable, rw);
		generateHeader(context, dataTable, rw);
		generateBody(context, dataTable, rw);
		generateFooter(context, dataTable, rw);
		new AJAXRenderer().generateBootsFacesAJAXAndJavaScriptForJQuery(context, component, rw,
				"." + clientId.replace(":", "") + "Table", null);
	}

	private void generateFooter(FacesContext context, DataTable dataTable, ResponseWriter rw) throws IOException {
		boolean hasFooter = false;
		boolean hasSearchbar = false;
		if (dataTable.isMultiColumnSearch()) {
			String position = dataTable.getMultiColumnSearchPosition();
			if ("both".equalsIgnoreCase(position) || "bottom".equalsIgnoreCase(position)) {
				hasSearchbar = true;
			}
		}
		for (UIComponent column : dataTable.getChildren()) {
			if (!column.isRendered()) {
				continue;
			}
			hasFooter |= column.getFacet("footer") != null;
		}
		if (hasFooter || hasSearchbar) {
			rw.startElement("tfoot", dataTable);
			if (hasSearchbar) {
				generateMultiColumnSearchRow(context, dataTable, rw);
			}
			if (hasFooter) {
				for (UIComponent column : dataTable.getChildren()) {
					if (!column.isRendered()) {
						continue;
					}
					rw.startElement("th", dataTable);
					Object footerStyle = column.getAttributes().get("footerStyle");
					if (footerStyle != null) {
						rw.writeAttribute("style", footerStyle, null);
					}
					Object footerStyleClass = column.getAttributes().get("footerStyleClass");
					if (footerStyleClass != null) {
						rw.writeAttribute("class", footerStyleClass, null);
					}
					if (column.getFacet("footer") != null) {
						UIComponent facet = column.getFacet("footer");
						facet.encodeAll(context);
					}
					rw.endElement("th");
				}
			}
			rw.endElement("tfoot");
		}
	}

	private void generateMultiColumnSearchRow(FacesContext context, DataTable dataTable, ResponseWriter rw)
			throws IOException {
		rw.startElement("tr", dataTable);
		List<UIComponent> columns = dataTable.getChildren();
		for (UIComponent column : columns) {
			if (!column.isRendered()) {
				continue;
			}
			rw.startElement("td", dataTable);
			Object footerStyle = column.getAttributes().get("footerStyle");
			if (footerStyle != null) {
				rw.writeAttribute("style", footerStyle, null);
			}
			Object searchable = column.getAttributes().get("searchable");
			if (searchable == null || ((searchable instanceof Boolean) && ((Boolean) searchable).equals(Boolean.TRUE))
					|| ((searchable instanceof String) && ((String) searchable).equalsIgnoreCase("true"))) {
				Object footerStyleClass = column.getAttributes().get("footerStyleClass");
				if (footerStyleClass != null) {
					rw.writeAttribute("class", "bf-multisearch " + footerStyleClass, null);
				} else {
					rw.writeAttribute("class", "bf-multisearch", null);
				}
				if (column.getFacet("header") != null) {
					UIComponent facet = column.getFacet("header");
					facet.encodeAll(context);
				} else if (column.getAttributes().get("label") != null) {
					rw.writeText(column.getAttributes().get("label"), null);
				}
			}
			rw.endElement("td");
		}
		rw.endElement("tr");
	}

	private void generateBody(FacesContext context, DataTable dataTable, ResponseWriter rw) throws IOException {
		rw.startElement("tbody", dataTable);
		int rows = dataTable.getRowCount();
		int visibleRowIndex = 0;
		dataTable.setRowIndex(-1);
		Object selectedRow = dataTable.getSelectedRow();
		for (int row = 0; row < rows; row++) {
			dataTable.setRowIndex(row);
			if (dataTable.isRowAvailable()) {
				rw.startElement("tr", dataTable);
				String rowStyleClass = dataTable.getRowStyleClass();

				if (null != selectedRow) {
					if ((dataTable.getRowData() == selectedRow) || (selectedRow.equals(dataTable.getRowData()))) {
						if (null == rowStyleClass) {
							rowStyleClass = "bf-selected-row";
						} else {
							rowStyleClass += "bf-selected-row";
						}
					}
				}
				if (null != rowStyleClass) {
					if (rowStyleClass.indexOf(",") >= 0) {
						String[] styleClasses = rowStyleClass.split(",");
						rowStyleClass = styleClasses[visibleRowIndex % styleClasses.length];
					}
					rowStyleClass = rowStyleClass.trim();
					if (rowStyleClass.length() > 0) {
						rw.writeAttribute("class", rowStyleClass, null);
					}
				}
				List<UIComponent> columns = dataTable.getChildren();
				for (UIComponent column : columns) {
					if (!column.isRendered()) {
						continue;
					}
					rw.startElement("td", dataTable);
					Object contentStyle = column.getAttributes().get("contentStyle");
					Object style = column.getAttributes().get("style");
					if (contentStyle != null && style == null) {
						rw.writeAttribute("style", contentStyle, null);
					} else if (contentStyle == null && style != null) {
						rw.writeAttribute("style", style, null);
					} else if (contentStyle != null && style != null) {
						rw.writeAttribute("style", style + ";" + contentStyle, null);
					}
					Object contentStyleClass = column.getAttributes().get("contentStyleClass");
					Object styleClass = column.getAttributes().get("styleClass");
					if (contentStyleClass != null && styleClass == null) {
						rw.writeAttribute("class", contentStyleClass, null);
					} else if (contentStyleClass == null && styleClass != null) {
						rw.writeAttribute("class", styleClass, null);
					} else if (contentStyleClass != null && styleClass != null) {
						rw.writeAttribute("class", styleClass + " " + contentStyleClass, null);
					}

					Object dataOrder = column.getAttributes().get("dataOrder");
					if (dataOrder != null)
						rw.writeAttribute("data-order", dataOrder, null);
					Object dataSearch = column.getAttributes().get("dataSearch");
					if (dataSearch != null)
						rw.writeAttribute("data-search", dataSearch, null);

					Object value = column.getAttributes().get("value");
					if (value != null) {
						rw.writeText(value, null);
					}

					renderChildrenOfColumn(column, context);
					rw.endElement("td");
				}
				rw.endElement("tr");
				visibleRowIndex++;
			}
		}
		rw.endElement("tbody");
		dataTable.setRowIndex(-1);
	}

	private void generateCaption(FacesContext context, DataTable dataTable, ResponseWriter rw) throws IOException {
		boolean hasCaption = dataTable.getCaption() != null;

		if (hasCaption) {
			rw.startElement("caption", dataTable);
			rw.writeText(dataTable.getCaption(), null);
			rw.endElement("caption");
		}
	}

	private void renderChildrenOfColumn(UIComponent column, FacesContext context) throws IOException {
		resetClientIdCacheRecursively(column);
		column.encodeChildren(context);
	}

	private void resetClientIdCacheRecursively(UIComponent c) {
		String id = c.getId();
		if (null != id) {
			c.setId(id); // this strange operation clears the cache of the clientId
		}
		Iterator<UIComponent> children = c.getFacetsAndChildren();
		if (children != null) {
			while (children.hasNext()) {
				UIComponent kid = children.next();
				resetClientIdCacheRecursively(kid);
			}
		}
	}

	private void generateHeader(FacesContext context, DataTable dataTable, ResponseWriter rw) throws IOException {
		rw.startElement("thead", dataTable);
		// Putting input fields into the header doesn't work yet
		if (dataTable.isMultiColumnSearch()) {
			String position = dataTable.getMultiColumnSearchPosition();
			if ("both".equalsIgnoreCase(position) || "top".equalsIgnoreCase(position)) {
				generateMultiColumnSearchRow(context, dataTable, rw);
			}
		}
		if (dataTable.getFacet("header") != null) {
			UIComponent facet = dataTable.getFacet("header");
			facet.encodeAll(context);
			rw.endElement("thead");
			return;
		}

		rw.startElement("tr", dataTable);
		int index = 0;
		List<UIComponent> columns = dataTable.getChildren();
		for (UIComponent column : columns) {
			if (!column.isRendered()) {
				continue;
			}
			rw.startElement("th", dataTable);
			Object headerStyle = column.getAttributes().get("headerStyle");
			Object style = column.getAttributes().get("style");

			if (headerStyle != null && style == null) {
				rw.writeAttribute("style", headerStyle, null);
			} else if (headerStyle == null && style != null) {
				rw.writeAttribute("style", style, null);
			} else if (headerStyle != null && style != null) {
				rw.writeAttribute("style", style + ";" + headerStyle, null);
			}
			Object headerStyleClass = column.getAttributes().get("headerStyleClass");
			Object styleClass = column.getAttributes().get("styleClass");
			if (headerStyleClass != null && styleClass == null) {
				rw.writeAttribute("class", headerStyleClass, null);
			} else if (headerStyleClass == null && styleClass != null) {
				rw.writeAttribute("class", styleClass, null);
			} else if (headerStyleClass != null && styleClass != null) {
				rw.writeAttribute("class", styleClass + " " + headerStyleClass, null);
			}
			if (column.getFacet("header") != null) {
				UIComponent facet = column.getFacet("header");
				facet.encodeAll(context);
			} else if (column.getAttributes().get("label") != null) {
				String labelStyleClass = (String) column.getAttributes().get("labelStyleClass");
				String labelStyle = (String) column.getAttributes().get("labelStyle");
				if (null != labelStyle || null != labelStyleClass) {
					rw.startElement("span", null);
					writeAttribute(rw, "style", labelStyle);
					writeAttribute(rw, "class", labelStyleClass);
				}
				rw.writeText(column.getAttributes().get("label"), null);
				if (null != labelStyle || null != labelStyleClass) {
					rw.endElement("span");
				}
			} else {
				boolean labelHasBeenRendered = false;
				for (UIComponent c : column.getChildren()) {
					if (c.getAttributes().get("label") != null) {
						String labelStyleClass = (String) c.getAttributes().get("labelStyleClass");
						String labelStyle = (String) c.getAttributes().get("labelStyle");
						if (null != labelStyle || null != labelStyleClass) {
							rw.startElement("span", null);
							writeAttribute(rw, "style", labelStyle);
							writeAttribute(rw, "class", labelStyleClass);
						}
						rw.writeText(c.getAttributes().get("label"), null);
						if (null != labelStyle || null != labelStyleClass) {
							rw.endElement("span");
						}
						labelHasBeenRendered = true;
						break;
					}
				}
				if (!labelHasBeenRendered) {
					ValueExpression ve = column.getValueExpression("value");
					if (null != ve) {
						String exp = ve.getExpressionString();
						int pos = exp.lastIndexOf('.');
						if (pos > 0) {
							exp = exp.substring(pos + 1);
						}
						exp = exp.substring(0, 1).toUpperCase() + exp.substring(1);
						String labelStyleClass = (String) column.getAttributes().get("labelStyleClass");
						String labelStyle = (String) column.getAttributes().get("labelStyle");
						if (null != labelStyle || null != labelStyleClass) {
							rw.startElement("span", null);
							writeAttribute(rw, "style", labelStyle);
							writeAttribute(rw, "class", labelStyleClass);
						}
						rw.writeText(exp.substring(0, exp.length() - 1), null);
						if (null != labelStyle || null != labelStyleClass) {
							rw.endElement("span");
						}
						labelHasBeenRendered = true;
					}
				}
				if (!labelHasBeenRendered) {
					rw.writeText("Column #" + index, null);
				}
			}
			String order = null;
			if (column.getFacet("order") != null) {
				UIComponent facet = column.getFacet("order");
				order = facet.toString();
			} else if (column.getAttributes().get("order") != null) {
				order = (String) column.getAttributes().get("order");
			}
			if (null != order) {
				order = order.trim();
				if ((!"asc".equals(order)) && (!"desc".equals(order))) {
					throw new FacesException("Invalid column order. Legal values are 'asc' and 'desc'.");
				}
				Map<Integer, String> columnSortOrder;
				if (dataTable.getColumnSortOrderMap() == null) {
					dataTable.initColumnSortOrderMap();
				}
				columnSortOrder = dataTable.getColumnSortOrderMap();
				columnSortOrder.put(index, order);
			}
			if (column.getAttributes().get("orderBy") != null) {
				String orderBy = (String) column.getAttributes().get("orderBy");
				updateColumnDefinition(dataTable, index, "'orderDataType': '" + orderBy + "'");

			}
			if (column.getAttributes().get("dataType") != null) {
				String type = (String) column.getAttributes().get("dataType");
				updateColumnDefinition(dataTable, index, "'type': '" + type + "'");
			}
			if (column.getAttributes().get("orderable") != null) {
				String orderable = column.getAttributes().get("orderable").toString();

				if ("false".equalsIgnoreCase(orderable)) {
					updateColumnDefinition(dataTable, index, "'orderable': false");
				}
			}
			if (column.getAttributes().get("searchable") != null) {
				String orderable = column.getAttributes().get("searchable").toString();

				if ("false".equalsIgnoreCase(orderable)) {
					updateColumnDefinition(dataTable, index, "'searchable': false");
				}
			}

			if (column.getAttributes().get("width") != null) {
				String width = column.getAttributes().get("width").toString();
				if (isNumeric(width)) {
					width += "px";
				}
				updateColumnDefinition(dataTable, index, "'width':'" + width + "'");
			}

			if (column.getAttributes().get("customOptions") != null) {
				String customOptions = column.getAttributes().get("customOptions").toString();
				updateColumnDefinition(dataTable, index, customOptions);
			}
			rw.endElement("th");
			index++;
		}
		rw.endElement("tr");

		rw.endElement("thead");
	}

	/**
	 * This methods generates the HTML code of the current b:dataTable.
	 * <code>encodeBegin</code> generates the start of the component. After the, the
	 * JSF framework calls <code>encodeChildren()</code> to generate the HTML code
	 * between the beginning and the end of the component. For instance, in the case
	 * of a panel component the content of the panel is generated by
	 * <code>encodeChildren()</code>. After that, <code>encodeEnd()</code> is called
	 * to generate the rest of the HTML code.
	 *
	 * @param context   the FacesContext.
	 * @param component the current b:dataTable.
	 * @throws IOException thrown if something goes wrong when writing the HTML
	 *                     code.
	 */
	@Override
	public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
		if (!component.isRendered()) {
			return;
		}
		DataTable dataTable = (DataTable) component;
		Map<Integer, String> columnSortOrder = dataTable.getColumnSortOrderMap();
		int pageLength = dataTable.getPageLength();
		String orderString = "[]";
		if (columnSortOrder != null) {
			StringBuilder sb = new StringBuilder();
			int i = 0;
			for (Map.Entry<Integer, String> entry : columnSortOrder.entrySet()) {
				String separator = (i > 0) ? "," : "";
				sb.append(separator).append("[").append(entry.getKey()).append(",").append("'").append(entry.getValue())
						.append("'").append("]");
				i++;
			}
			orderString = sb.toString();
		}
		ResponseWriter rw = context.getResponseWriter();
		String clientIdRaw = dataTable.getClientId();
		String clientId = clientIdRaw.replace(":", "");
		String widgetVar = dataTable.getWidgetVar();
		if (null == widgetVar) {
			widgetVar = BsfUtils.widgetVarName(clientIdRaw);
		}
		String lang = determineLanguage(context, dataTable);
		rw.endElement("table");
		endDisabledFieldset(dataTable, rw);
		String responsiveStyle = Responsive.getResponsiveStyleClass(dataTable, false);
		if (null != responsiveStyle && responsiveStyle.trim().length() > 0) {
			rw.endElement("div");
		}
		if (dataTable.isScrollHorizontally()) {
			rw.endElement("div");
		}
		Tooltip.activateTooltips(context, dataTable);
		rw.startElement("script", component);
		// # Start enclosure
		rw.writeText("$(document).ready(function() {", null);
		// # Enclosure-scoped variable initialization
		String options = "";
		options = addOptions("fixedHeader: " + dataTable.isFixedHeader(), options);
		options = addOptions("responsive: " + dataTable.isResponsive(), options);
		options = addOptions("paging: " + dataTable.isPaginated(), options);
		if (!dataTable.isInfo()) {
			options = addOptions("info: false", options);
		}
		options = addOptions("pageLength: " + pageLength, options);
		options = addOptions("lengthMenu: " + getPageLengthMenu(dataTable), options);
		options = addOptions("searching: " + dataTable.isSearching(), options);
		options = addOptions("order: " + orderString, options);
		options = addOptions("stateSave: " + dataTable.isSaveState(), options);
		options = addOptions("mark: true", options);

		if (dataTable.isSelect()) {
			String json = "";
			String items = dataTable.getSelectedItems();
			if ("column".equals(items) || "columns".equals(items)) {
				json += "items:'column',";
			} else if ("cell".equals(items) || "cells".equals(items)) {
				json += "items:'cell',";
			}
			if ("single".equalsIgnoreCase(dataTable.getSelectionMode())) {
				json += "style:'single',";
			} else {
				json += "style:'os',";
			}
			if (!dataTable.isSelectionInfo()) {
				json += "info:false,";
			}
			if (dataTable.isDeselectOnBackdropClick()) {
				json += "blurable:true,";
			}
			if (json.length() > 1) {
				json = "select:{" + json.substring(0, json.length() - 1) + "}";
			} else {
				json = "select:true";
			}
			options = addOptions(json, options);
		}

		options = addOptions(generateScrollOptions(dataTable), options);
		options = addOptions((BsfUtils.isStringValued(lang) ? "  language: { url: '" + lang + "' } " : null), options);
		options = addOptions(generateColumnInfos(dataTable.getColumnInfo()), options);
		options = addOptions(dataTable.getCustomOptions(), options);
		options = addOptions(getButtons(dataTable), options);
		String selectCommand = "";
		Object selectedRow = dataTable.getSelectedRow();
		if (null != selectedRow) {
			String selector = "'.bf-selected-row'";
			if (selectedRow instanceof String) {
				try {
					Integer.parseInt((String) selectedRow);
					selector = (String) selectedRow;
				} catch (NumberFormatException itIsAString) {
					selector = "'" + selectedRow + "'";
				}
			} else if (selectedRow instanceof Number) {
				selector = selectedRow.toString();
			}
			selectCommand = widgetVar + ".DataTable().rows(" + selector + ").select(); ";
		}
		Object selectedColumn = dataTable.getSelectedColumn();
		if (null != selectedColumn) {
			String selector = "'.bf-selected-column'";
			if (selectedColumn instanceof String) {
				try {
					Integer.parseInt((String) selectedColumn);
					selector = (String) selectedColumn;
				} catch (NumberFormatException itIsAString) {
					selector = "'" + selectedColumn + "'";
				}
			} else if (selectedColumn instanceof Number) {
				selector = selectedColumn.toString();
			}
			selectCommand += widgetVar + ".DataTable().columns(" + selector + ").select(); ";
		}
		if (selectCommand.length() > 0) {
			options = addOptions("'initComplete': function( settings, json ) { " + selectCommand + "}", options);
		}

		if (dataTable.getRowGroup() != null) {
			String rowGroup = dataTable.getRowGroup();
			try {
				Integer.parseInt(rowGroup);
				options = addOptions("orderFixed: [" + rowGroup + ", 'asc']", options);
				rowGroup = "rowGroup:{dataSrc:" + rowGroup + "}";
			} catch (NumberFormatException itsJson) {
				// consider it a Json object
			}
			options = addOptions(rowGroup, options);
		}

		rw.writeText(widgetVar + " = $('." + clientId + "Table" + "');" +
		// # Get instance of wrapper, and replace it with the unwrapped table.
				"var wrapper = $('#" + clientIdRaw.replace(":", "\\\\:") + "_wrapper');" + "wrapper.replaceWith("
				+ widgetVar + ");" + "var table = " + widgetVar + ".DataTable({" + options + "});", null);

		if (dataTable.isMultiColumnSearch()) {
			// # Footer stuff:
			// https://datatables.net/examples/api/multi_filter.html
			// # Convert footer column text to input textfields

			String filter = "<div class=\"form-group has-feedback\">";
			filter += "<input class=\"form-control input-sm datatable-filter-field\" type=\"text\" placeholder=\"' + title + '\" />";
			filter += "<i class=\"fa fa-search form-control-feedback\"></i>";
			filter += "</div>";

			rw.writeText(widgetVar + ".find('.bf-multisearch').each(function(){" + "var title=$(this).text();"
					+ "$(this).html('" + filter + "');" + "});", null);
			// # Add event listeners for each multisearch input
			rw.writeText("var inputs=$(" + widgetVar + ".find('.bf-multisearch input'));", null);
			rw.writeText("table.columns().every( function(col) {" + "var that=this;if(col<inputs.length){"
					+ "inputs[col].value=table.columns(col).search()[0];"
					+ "$(inputs[col]).on('keyup change', function(){if(that.search()!==this.value){"
					+ "that.search(this.value).draw('page');}});}", null);
			rw.writeText("});", null);
			int col = 0;
			for (UIComponent column : dataTable.getChildren()) {
				if (!column.isRendered()) {
					continue;
				}
				String searchValue = null;
				if ((column instanceof DataTableColumn)) {
					searchValue = ((DataTableColumn) column).getSearchValue();
					if (!((DataTableColumn) column).isSearchable()) {
						continue;
					}
				} else {
					Object sv = column.getAttributes().get("searchValue");
					if (sv != null && (!"".equals(sv))) {
						searchValue = sv.toString();
					}
				}
				if (null != searchValue && searchValue.length() > 0) {
					rw.writeText("inputs[" + col + "].value='" + searchValue + "';", null);
					rw.writeText("table.columns(" + col + ").search('" + searchValue + "').draw('page');", null);
				}
				col++;
			}
		}
		// # End enclosure
		rw.writeText("} );", null);
		rw.endElement("script");
	}

	private String getButtons(DataTable dataTable) {
		StringBuilder b = new StringBuilder();
		if (dataTable.isColumnVisibility()) {
			b.append("'colvis',");
		}
		if (dataTable.isCopy()) {
			b.append("'copy',");
		}
		if (dataTable.isCsv()) {
			b.append("'csv',");
		}
		if (dataTable.isExcel()) {
			b.append("'excel',");
		}
		if (dataTable.isPdf()) {
			b.append("'pdf',");
		}
		if (dataTable.isPrint()) {
			b.append("'print',");
		}
		if (b.length() > 0) {
			return "dom: '<\"col-sm-6\"l><\"col-sm-6\"f>rtiBp'," + "buttons: [" + b.substring(0, b.length() - 1) + "]";
		}
		return null;
	}

	private String addOptions(String newOption, String options) {
		if (newOption != null && newOption.length() > 0) {
			if (options.length() > 0)
				options += ",";
			options += newOption;
		}
		return options;
	}

	private String generateScrollOptions(DataTable dataTable) {
		String scrollY = dataTable.getScrollSize();
		boolean scrollX = dataTable.isScrollX();
		if (null == scrollY && (!scrollX)) {
			return "";
		}
		String result = "";
		if (null != scrollY) {
			if (!NUMERIC_PATTERN.matcher(scrollY).matches()) {
				// you can pass the scrollY either as a numeric value (in which
				// case it is the height in px)
				// or as a String containing the unit. If it's a String, it has
				// to be surround be ticks.
				scrollY = "'" + scrollY + "'";
			}
			result += " scrollY: " + scrollY + ",";
		}
		if (scrollX) {
			result += "scrollX: true,";
		}

		return result + "scrollCollapse: " + dataTable.isScrollCollapse();
	}

	private String generateColumnInfos(List<String> columnInfo) {
		if (columnInfo == null) {
			return "";
		}
		String result = "columns: [";
		for (String col : columnInfo) {
			if (null == col) {
				result += "null,";
			} else {
				result += "{" + col + "},";
				if (col.contains("dom-text")) {
					if (!col.contains("type")) {
						throw new FacesException(
								"You have to specify the data type of the column if you want to sort it using order-by.");
					}
				}
			}
		}
		result = result.substring(0, result.length() - 1); // remove the
															// trailing comma
		result += "]";
		return result;
	}

	private String getPageLengthMenu(DataTable dataTable) {
		String menu = dataTable.getPageLengthMenu();
		if (menu != null) {
			menu = menu.trim();
			if (!menu.startsWith("[")) {
				menu = "[" + menu;
			}
			if (!menu.endsWith("]")) {
				menu = menu + "]";
			}
		}
		return menu;
	}

	/**
	 * Determine if the user specify a lang Otherwise return null to avoid language
	 * settings.
	 *
	 * @param fc
	 * @param dataTable
	 * @return
	 */
	private String determineLanguage(FacesContext fc, DataTable dataTable) {
		final List<String> availableLanguages = Arrays.asList("de", "en", "es", "fr", "hu", "it", "nl", "pl", "pt",
				"ru");
		if (BsfUtils.isStringValued(dataTable.getCustomLangUrl())) {
			return dataTable.getCustomLangUrl();
		} else if (BsfUtils.isStringValued(dataTable.getLang())) {
			String lang = dataTable.getLang();
			if (availableLanguages.contains(lang)) {
				return determineLanguageUrl(fc, lang);
			}
		} else {
			String lang = fc.getViewRoot().getLocale().getLanguage();
			if (availableLanguages.contains(lang)) {
				return determineLanguageUrl(fc, lang);
			}
		}
		return null;
	}

	/**
	 * Determine the locale to set-up to dataTable component. The locale is
	 * determined in this order: - if customLangUrl is specified, it is the value
	 * set up - otherwise, the system check if locale is explicit specified -
	 * otherwise it takes from the ViewRoot
	 *
	 * @param fc
	 * @param dataTable
	 * @return
	 */
	private String determineLanguageUrl(FacesContext fc, String lang) {
		// Build resource url
		return fc.getApplication().getResourceHandler()
				.createResource("jq/ui/i18n/dt/datatable-" + lang + ".json", C.BSF_LIBRARY).getRequestPath();
	}

	@Override
	public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
		// Children are already rendered in encodeBegin()
	}

	protected void initColumnInfos(DataTable dataTable) {
		if (dataTable.getColumnInfo() == null) {
			List<String> infos = new ArrayList<String>();

			for (int k = 0; k < dataTable.getChildren().size(); k++) {

				if (dataTable.getChildren().get(k).isRendered()) {
					infos.add(null);
				}

			}

			dataTable.setColumnInfo(infos);
		}
	}

	protected void updateColumnDefinition(DataTable dataTable, int index, String value) {
		initColumnInfos(dataTable);

		List<String> infos = dataTable.getColumnInfo();

		String s = infos.get(index);

		if (s == null) {
			infos.set(index, value);
		} else {
			infos.set(index, s + "," + value);
		}

	}

	public static boolean isNumeric(String str) {
		for (char c : str.toCharArray()) {
			if (!Character.isDigit(c))
				return false;
		}
		return true;
	}

}
