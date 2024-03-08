package com.sap.cds.feature.attachments.handler.common;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Expand;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.StructuredType;
import com.sap.cds.ql.cqn.CqnDelete;
import com.sap.cds.ql.cqn.CqnPredicate;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.services.persistence.PersistenceService;

public class DefaultAttachmentsReader implements AttachmentsReader {

	private final AssociationCascader cascader;

	public DefaultAttachmentsReader(AssociationCascader cascader, PersistenceService persistence) {
		this.cascader = cascader;
		this.persistence = persistence;
	}

	private final PersistenceService persistence;

	@Override
	public List<CdsData> readAttachments(CdsModel model, CdsEntity entity, CqnDelete delete) {
		var dataList = new ArrayList<CdsData>();

		var resultWhere = getWhere(delete);
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

	private Optional<CqnPredicate> getWhere(CqnDelete delete) {
		var filter = delete.ref().asRef().rootSegment().filter();
		var where = delete.where();
		if (filter.isPresent() && where.isPresent()) {
			return Optional.of(CQL.and(filter.get(), where.get()));
		} else if (filter.isPresent()) {
			return filter;
		} else {
			return where;
		}
	}

	private Optional<CqnSelect> getSelectStatement(LinkedList<AssociationIdentifier> path, Optional<CqnPredicate> where, CdsEntity entity) {
		var listEntry = path.stream().filter(entry -> entry.fullEntityName().equals(entity.getQualifiedName())).findAny();
		var resultSelect = new AtomicReference<CqnSelect>();
		listEntry.ifPresent(entry -> {

			var descendingIterator = path.descendingIterator();

			Function<StructuredType<?>, Expand<?>> func = null;

			while (descendingIterator.hasNext()) {
				var next = descendingIterator.next();
				if (next.fullEntityName().equals(listEntry.get().fullEntityName())) {
					break;
				}

				if (Objects.isNull(func)) {
					func = item -> item.to(next.associationName()).expand();
				} else {
					Function<StructuredType<?>, Expand<?>> finalFunc = func;
					func = item -> item.to(next.associationName()).expand((Function) finalFunc);
				}
			}

			CqnSelect selectFunc = where.isPresent() ? Select.from(RootTable_.CDS_NAME).where(where.get()).columns(func)
																												: Select.from(RootTable_.CDS_NAME).columns(func);
			resultSelect.set(selectFunc);
		});
		return Optional.ofNullable(resultSelect.get());
	}
}
