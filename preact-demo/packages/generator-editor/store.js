import pTap                 from 'p-tap';

import tokenizer            from '../tokenizer/tokenizer';
import tokensToBlockly      from '../tokenizer/json-to-blockly';


const EXAMPLE_XML = '<xml xmlns="http://www.w3.org/1999/xhtml"><block type="segment" x="49" y="122"><field name="GOAL">description</field><statement name="CHILDREN"><block type="unordered-list"><mutation input_count="4"></mutation><value name="CHILD0"><block type="attribute"><field name="ATTRIBUTE">color</field></block></value><value name="CHILD1"><block type="attribute"><field name="ATTRIBUTE">material</field></block></value><value name="CHILD2"><block type="attribute"><field name="ATTRIBUTE">make</field></block></value></block></statement></block></xml>';


export default {

    getInitialState: () => ({
        blocklyXml:         '',
        contextName:        null,
        dataSample:         null,
        generatorName:      'Example Generator',
        tokenizerError:     null,
        tokenizerLoading:   false,
    }),

    onChangeContext: ({ contextName }) => ({
        contextName,
    }),

    onChangeBlocklyWorkspace: blocklyXml => ({
        blocklyXml,
    }),

    onClickAddOnboardSegment: () => ({
        blocklyXml:         EXAMPLE_XML,
    }),

    onClickUpload: ({ dataSample }) => ({
        dataSample,
    }),

    onTokenizerRequest: () => ({
        tokenizerLoading:   true,
    }),

    onTokenizerResult: blocklyXml => ({
        blocklyXml,
        tokenizerError:     false,
        tokenizerLoading:   false,
    }),

    onTokenizerError: tokenizerError => ({
        tokenizerError,
        tokenizerLoading:   false,
    }),

    onSubmitTextExample: ({ text }, { events, state }) => {

        /// Prevent new requests while the previous one is not finished:
        if( state.tokenizerLoading ) {
            return;
        }

        events.onTokenizerRequest();

        tokenizer( text )
            .then( tokensToBlockly )
            .then( events.onTokenizerResult )
            .catch( pTap( console.error ))
            .catch( events.onTokenizerError );
    },
};
