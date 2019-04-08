import debugConsole         from '../../qa-utils/debug-console';
import nlgProvide           from '../../nlg-api/provide-response';
import requestInterceptor   from '../../qa-utils/request-interceptor';

import EMPTY_LEXICON_LIST   from '../data/empty-lexicon-list';
import USER                 from '../data/user';


const { TEST_URL } =        process.env;


export default async ( t, run, ...args ) => {

    debugConsole( t.page );

    const interceptor =     await requestInterceptor( t.page );
    const {
        continueAll,
        provideOnce,
        stopInterception,
    } = interceptor;
    const nlgProvideOnce =  nlgProvide( provideOnce );

    continueAll( 'GET', new RegExp( `${ TEST_URL }/.*` ));

    t.page.goto( TEST_URL );

    await Promise.all([
        nlgProvideOnce( 'GET', `/data/?user=${ USER.id }`, []),
        nlgProvideOnce( 'GET', '/document-plans/', []),
        nlgProvideOnce( 'GET', '/lexicon?', EMPTY_LEXICON_LIST ),
    ]);

    if( run ) {
        await run( Object.assign( t, { interceptor, nlgProvideOnce }), ...args );
    }

    await stopInterception( t.page );
};
