import itemsWithStatus  from '../store-utils/items-with-status';


const storeFns =            itemsWithStatus( 'id', 'fileItems', 'statuses' );
export const getItem =      storeFns.getItem;
export const getStatus =    storeFns.getStatus;
export const patchItem =    storeFns.patchItem;
export const patchStatus =  storeFns.patchStatus;

export const addItemFields = item => ({
    ...item,
    contentType:    item.contentType || 'text/csv',
    fileName:       item.key.split( '/' ).pop(),
    id:             item.key,
});

export const findFileByPlan  = ( dataSamples, plan ) => (
    plan && plan.dataSampleId
    && dataSamples.fileItems[plan.dataSampleId]
);

export const statusTemplate = {
    getDataError:   null,
    getDataLoading: false,
};

export const getPlanDataRow = ( fileItem, plan ) => (
    fileItem
    && fileItem.data
    && plan
    && fileItem.data[ plan.dataSampleRow || 0 ]
);
