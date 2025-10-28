using { com.sap.cds.incidents as my } from 'com.sap.cds/incident_management';
using { sap.attachments.Attachments } from 'com.sap.cds/cds-feature-attachments';

extend my.Incidents {
    attachments: Composition of many Attachments;
}