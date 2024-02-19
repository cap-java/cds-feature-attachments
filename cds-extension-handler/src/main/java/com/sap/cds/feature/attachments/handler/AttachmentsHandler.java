package com.sap.cds.feature.attachments.handler;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.model.ModelConstants;
import com.sap.cds.feature.attachments.service.AttachmentAccessException;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnSelectListItem;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsElementDefinition;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CdsUpdateEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@ServiceName(value = "*", type = ApplicationService.class)
public class AttachmentsHandler implements EventHandler {

    private final AttachmentService attachmentService;

    public AttachmentsHandler(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @After
    @HandlerOrder(HandlerOrder.EARLY)
    void readAttachments(CdsReadEventContext context, List<CdsData> data) {
        //1. check if field with annotation @Core.MediaType is present in target
        //2. check if target is annotated with IsMediaData
        //3. check if request shall request attachment content or attachment entity without file
        //     check: is KEY field of target in requested fields, if no content is requested
        //     if attachment content -> call attachmentService to read InputStream
        //     if attachment entity -> set dummy value for content field

        var contentFieldOptional = findContentField(context);
        if (contentFieldOptional.isPresent() && hasModelDataAnnotation(context)) {
            var contentField = contentFieldOptional.get();

            var keyField = context.getTarget().elements().filter(CdsElementDefinition::isKey).findAny().orElseThrow();

            if (hasCqnField(context.getCqn(), contentField.getName())) {
                if (hasCqnField(context.getCqn(), keyField.getName())) {
                    data.forEach(entry -> {
                        if (Objects.nonNull(entry.get(contentField.getName()))) {
                            entry.put(contentField.getName(), "empty_value");
                        }
                    });
                } else {
                    //single value request
                    if (data.size() == 1) {
                        var firstDataRow = data.get(0);
                        try {
                            var documentStream = attachmentService.readAttachment(context);
                            firstDataRow.put(contentField.getName(), documentStream);
                            context.setResult(data);
                        } catch (AttachmentAccessException exception) {
                            throw new ServiceException(exception);
                        }
                    }
                }
            }
        }
    }

    @On
    @HandlerOrder(HandlerOrder.EARLY)
    public void uploadAttachment(CdsUpdateEventContext context, List<CdsData> cdsData) {
        var contentFieldOptional = findContentField(context);
        if (contentFieldOptional.isPresent() && hasModelDataAnnotation(context)) {
            cdsData.forEach(data -> {
                if (data.containsKey(contentFieldOptional.get().getName())) {
                    // stream data is uploaded
                    // call AttachementsService.put()
                    // make sure other properties are stored in db
                }
            });
        }
    }

    private Optional<CdsElement> findContentField(EventContext context) {
        return context.getTarget().elements()
                .filter(element -> element.annotations().anyMatch(anno -> anno.getName().equals("Core.MediaType")))
                .findAny();
    }

    private boolean hasModelDataAnnotation(EventContext context) {
        return context.getTarget().getAnnotationValue(ModelConstants.ANNOTATION_IS_MEDIA_DATA, false);
    }

    private boolean hasCqnField(CqnSelect cqn, String fieldName) {
        return cqn.items().stream().filter(CqnSelectListItem::isRef).map(CqnSelectListItem::asRef)
                .anyMatch(i -> i.path().equals(fieldName));
    }

}
