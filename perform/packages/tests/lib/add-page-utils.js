const SELECTOR_WAIT_OPTIONS = {
    timeout:            1e3,
};


export default ( t, run, ...args ) =>
    run(
        Object.assign( t, {

            clearInput: async ( selector, page = t.page ) => {
                await page.$eval( selector, el => {
                    el.focus();
                    el.setSelectionRange( 0, el.value.length );
                });
                await page.keyboard.press( 'Backspace' );
            },

            findElement: ( selector, page = t.page ) =>
                t.notThrowsAsync( page.waitForSelector( selector, SELECTOR_WAIT_OPTIONS )),

            getElementAttribute: ( selector, attributeName, page = t.page ) =>
                t.findElement( selector, page )
                    .then(() => page.$eval(
                        selector,
                        ( el, attributeName ) => el.getAttribute( attributeName ),
                        attributeName,
                    )),

            getElementProperty: ( selector, propertyName, page = t.page ) =>
                t.findElement( selector, page )
                    .then(() => page.$eval(
                        selector,
                        ( el, propertyName ) => el[propertyName],
                        propertyName,
                    )),

            getElementText: ( selector, page = t.page ) =>
                t.findElement( selector, page )
                    .then(() => page.$eval(
                        selector,
                        el => el.innerText,
                    )),

            getElementValue: ( selector, page = t.page ) =>
                t.findElement( selector, page )
                    .then(() => page.$eval(
                        selector,
                        el => el.value,
                    )),

            notFindElement: ( selector, page = t.page ) =>
                t.throwsAsync( page.waitForSelector( selector, SELECTOR_WAIT_OPTIONS )),

            resetMouse: ( page = t.page ) =>
                page.mouse.move( 0, 0 ),

            retypeElementText: async ( selector, value, options ) => {
                await t.clearInput( selector );
                await t.page.type( selector, value, options );
                await t.page.keyboard.press( 'Control' );
            },

            waitUntilElementGone: async ( selector, timeout = 10e3, page = t.page ) => {

                const timeoutTimestamp =    +new Date + timeout;

                while( +new Date < timeoutTimestamp ) {
                    try {
                        await page.waitForSelector( selector, { timeout: 500 });
                    } catch( _ ) {
                        return;
                    }
                }
                throw Error( `Timeout (${ timeout }ms) exceeded while waiting for selector ${ selector }` );
            },
        }),
        ...args,
    );
