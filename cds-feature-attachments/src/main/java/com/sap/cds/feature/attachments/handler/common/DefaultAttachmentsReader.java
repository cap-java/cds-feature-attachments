package com.sap.cds.feature.attachments.handler.common;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.sap.cds.CdsData;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.StructuredType;
import com.sap.cds.ql.cqn.CqnFilterableStatement;
import com.sap.cds.ql.cqn.CqnPredicate;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnSelectListItem;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.services.persistence.PersistenceService;

public class DefaultAttachmentsReader implements AttachmentsReader {

	private final AssociationCascader cascader;
	private final PersistenceService persistence;

	public DefaultAttachmentsReader(AssociationCascader cascader, PersistenceService persistence) {
		this.cascader = cascader;
		this.persistence = persistence;
	}

	@Override
	public List<CdsData> readAttachments(CdsModel model, CdsEntity entity, CqnFilterableStatement statement) {
		var resultWhere = ApplicationHandlerHelper.getWhere(statement);
		return getData(model, entity, resultWhere);
	}

	private ArrayList<CdsData> getData(CdsModel model, CdsEntity entity, Optional<CqnPredicate> resultWhere) {
		var dataList = new ArrayList<CdsData>();
		var pathLists = cascader.findEntityPath(model, entity);
		pathLists.forEach(path -> {
			var select = getSelectStatement(path, resultWhere, entity);
			select.ifPresent(selectStatement -> {
				var result = persistence.run(selectStatement);
				dataList.addAll(result.listOf(CdsData.class));
			});

		});

		return dataList;
	}

	private Optional<CqnSelect> getSelectStatement(LinkedList<AssociationIdentifier> path, Optional<CqnPredicate> where, CdsEntity entity) {
		var listEntry = path.stream().filter(entry -> entry.fullEntityName().equals(entity.getQualifiedName())).findAny();
		var resultSelect = new AtomicReference<CqnSelect>();
		listEntry.ifPresent(entry -> {

			var descendingIterator = path.descendingIterator();

			Function<StructuredType<?>, CqnSelectListItem> func = null;

			while (descendingIterator.hasNext()) {
				var next = descendingIterator.next();
				if (next.fullEntityName().equals(listEntry.get().fullEntityName())) {
					break;
				}

				if (Objects.isNull(func)) {
					func = item -> item.to(next.associationName()).expand();
				} else {
					var finalFunc = func;
					//TODO use list of expands and not function, see example from draft
					func = item -> item.to(next.associationName()).expand((Function) finalFunc);
				}
			}

			if (Objects.isNull(func)) {
				func = item -> item.to(entity.getQualifiedName())._all();
			}
			CqnSelect selectFunc = where.isPresent() ? Select.from(entry.fullEntityName()).where(where.get()).columns(func)
																												: Select.from(entry.fullEntityName()).columns(func);
			resultSelect.set(selectFunc);
		});
		return Optional.ofNullable(resultSelect.get());
	}

}
