import classnames       from 'classnames';
import { h }            from 'preact';

import useStores        from '../context/use-stores';

import Header           from './Header';
import OnboardCode      from './onboard/Code';
import OnboardData      from './onboard/Data';
import { QA }           from './qa.constants';
import S                from './PlanEditor.sass';
import Workspace        from './Workspace';


export default useStores([
    'planEditor',
])(({
    planEditor: {
        getVariants,
        workspaceXml,
    },
}) =>
    <div className={ S.className }>
        <Header className={ QA.HEADER } />
        <div className={ classnames( S.body, QA.BODY ) }>
            <OnboardData>
                <OnboardCode />
            </OnboardData>
            { workspaceXml &&
                <Workspace
                    onChangeWorkspace={ getVariants }
                    workspaceXml={ workspaceXml }
                />
            }
        </div>
    </div>
);
