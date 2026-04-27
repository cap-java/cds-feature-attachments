namespace mt.test.data;

using {cuid} from '@sap/cds/common';
using {sap.attachments.Attachments} from 'com.sap.cds/cds-feature-attachments';

entity Documents : cuid {
    title       : String;
    attachments : Composition of many Attachments;
}
