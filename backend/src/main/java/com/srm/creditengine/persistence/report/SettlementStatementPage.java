package com.srm.creditengine.persistence.report;

import java.util.List;

/**
 * @param content rows of the requested page
 * @param totalElements rows matching the filters across all pages
 */
public record SettlementStatementPage(List<SettlementStatementRow> content, long totalElements) {}
