import { h }            from 'preact';

import AtjReview        from '../atj-review/AtjReview';
import { QA }           from '../plan-editor/qa.constants';
import { useStores }    from '../vesa/';

import S                from './VariantReview.sass';


export default useStores([
    'planEditor',
    'variantsApi',
])(({
    planEditor: {
        documentPlan,
        workspaceXml,
    },
    variantsApi: {
        error,
        loading,
        result,
    },
}) =>
    <div className={ S.className }>
        <div className={ S.header }>
            [P]review
        </div>
        <div className={ S.body }>
            <div className={ S.hiddenItem }>
                { workspaceXml ? workspaceXml : 'No Blockly yet.' }
            </div>
            <div className={ S.hiddenItem }>
                <pre className={ QA.NLG_JSON }>
                    { documentPlan
                        ? JSON.stringify( documentPlan, null, 4 )
                        : 'No JSON yet.'
                    }
                </pre>
            </div>
            { error &&
                <div className={ S.itemError }>
                    { error }
                </div>
            }
            { loading &&
                <div className={ S.item }>Loading variants...</div>
            }
            { result && (
                !( result.variants && result.variants.length )
                    ? <div className={ S.item }>No variants</div>
                    : result.variants.map( element =>
                        <div className={ S.item }>
                            <AtjReview key={ element.id } element={ element } />
                        </div>
                    )
            )}
        </div>
    </div>
);
