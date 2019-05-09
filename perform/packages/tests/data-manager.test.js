import R                    from 'ramda';
import test                 from 'ava';

import {
    createDataFileData,
}   from './data/data-file-data';
import DATA_FILE_LIST       from './data/data-file-list';
import defaultResponsesPage from './lib/default-responses-page';
import noRecordsPage        from './lib/no-records-page';
import selectFile           from './lib/select-file';
import { SELECTORS }        from './constants';


test( 'default elements visible', defaultResponsesPage, async t => {
    t.timeout( 5e3 );

    await t.findElement( SELECTORS.DATA_MANAGER_FILE_ADD );
    await t.findElement( SELECTORS.DATA_MANAGER_FILE_DOWNLOAD );
    await t.findElement( SELECTORS.DATA_MANAGER_FILE_LIST );

    await t.notFindElement( SELECTORS.DATA_MANAGER_FILE_BROWSE );
    await t.notFindElement( SELECTORS.DATA_MANAGER_FILE_CLOSE );
    await t.notFindElement( SELECTORS.DATA_MANAGER_FILE_UPLOAD );

    await t.findElement( SELECTORS.DATA_MANAGER_CELL_BLOCK );
    await t.findElement( SELECTORS.DATA_MANAGER_CELL_NAME );
    await t.findElement( SELECTORS.DATA_MANAGER_CELL_TABLE );
    await t.findElement( SELECTORS.DATA_MANAGER_CELL_VALUE );

    await t.findElement( SELECTORS.DATA_MANAGER_ROW_NEXT );
    await t.findElement( SELECTORS.DATA_MANAGER_ROW_PREVIOUS );
    await t.findElement( SELECTORS.DATA_MANAGER_ROW_SELECT );
});


test( 'correct elements when no files', noRecordsPage, async t => {
    t.timeout( 5e3 );

    await t.notFindElement( SELECTORS.DATA_MANAGER_FILE_ADD );
    await t.notFindElement( SELECTORS.DATA_MANAGER_FILE_DOWNLOAD );
    await t.notFindElement( SELECTORS.DATA_MANAGER_FILE_LIST );

    await t.findElement( SELECTORS.DATA_MANAGER_FILE_BROWSE );
    await t.findElement( SELECTORS.DATA_MANAGER_FILE_CLOSE );
    await t.findElement( SELECTORS.DATA_MANAGER_FILE_UPLOAD );

    await t.notFindElement( SELECTORS.DATA_MANAGER_CELL_BLOCK );
    await t.notFindElement( SELECTORS.DATA_MANAGER_CELL_NAME );
    await t.notFindElement( SELECTORS.DATA_MANAGER_CELL_TABLE );
    await t.notFindElement( SELECTORS.DATA_MANAGER_CELL_VALUE );

    await t.notFindElement( SELECTORS.DATA_MANAGER_ROW_NEXT );
    await t.notFindElement( SELECTORS.DATA_MANAGER_ROW_PREVIOUS );
    await t.notFindElement( SELECTORS.DATA_MANAGER_ROW_SELECT );
});


test( 'can change file', defaultResponsesPage, async t => {
    t.timeout( 5e3 );

    const dataFileId =      DATA_FILE_LIST[2].key;
    const dataFileData =    createDataFileData({ prefix: t.title });

    await selectFile( t, dataFileId, dataFileData );

    await t.waitUntilElementGone( SELECTORS.UI_INFO );
    await t.waitUntilElementGone( SELECTORS.UI_LOADING );
    await t.notFindElement( SELECTORS.UI_ERROR );

    await t.findElement( SELECTORS.DATA_MANAGER_ROW_NEXT );
    await t.findElement( SELECTORS.DATA_MANAGER_ROW_PREVIOUS );
    await t.findElement( SELECTORS.DATA_MANAGER_ROW_SELECT );

    const selectValue =     await t.getElementValue( SELECTORS.DATA_MANAGER_FILE_LIST );
    t.is( selectValue, dataFileId );

    const downloadUrl =     await t.getElementAttribute( SELECTORS.DATA_MANAGER_FILE_DOWNLOAD, 'href' );
    t.regex( downloadUrl, new RegExp( `${ dataFileId }$` ));

    const isNextDisabled =  await t.getElementProperty( SELECTORS.DATA_MANAGER_ROW_NEXT, 'disabled' );
    t.is( isNextDisabled, dataFileData.data.length < 2 );

    const isPrevDisabled =  await t.getElementProperty( SELECTORS.DATA_MANAGER_ROW_PREVIOUS, 'disabled' );
    t.is( isPrevDisabled, true );

    const rowValue =        await t.getElementValue( SELECTORS.DATA_MANAGER_ROW_SELECT );
    t.is( rowValue, '0' );

    const firstCellName =   await t.getElementProperty( SELECTORS.DATA_MANAGER_CELL_NAME, 'innerText' );
    t.is( firstCellName, Object.keys( dataFileData.data[0])[0]);

    const firstCellValue =  await t.getElementProperty( SELECTORS.DATA_MANAGER_CELL_VALUE, 'innerText' );
    t.is( firstCellValue, R.values( dataFileData.data[0])[0]);
});


test.todo( 'correct number of cells visible' );
test.todo( 'correct cell names visible' );
test.todo( 'correct cell values visible' );

test.todo( 'can change cell value row' );
test.todo( 'row buttons correctly disabled' );


test.todo( 'can download file' );
test.todo( 'can upload file' );
