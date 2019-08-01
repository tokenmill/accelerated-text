import { h }                from 'preact';

import CURVE                from './curve';


export default ({ color, ...props }) =>
    <svg viewBox="0 0 36 30" { ...props }>
        <g transform="translate( 8, 0 )">
            <path
                d={ `m 0,0 H 36 v 30 H 0 V 22.5 ${ CURVE } z` }
                fill={ color }
            />
        </g>
    </svg>;
