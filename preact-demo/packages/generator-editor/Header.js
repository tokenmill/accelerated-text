import classnames       from 'classnames';
import { h }            from 'preact';

import useStores        from '../context/use-stores';

import S                from './Header.sass';
import SelectContext    from './SelectContext';
import UploadDataSample from './UploadDataSample';


export default useStores([
    'generatorEditor',
])(({
    className,
    generatorEditor: {
        generatorName,
    },
}) =>
    <div className={ classnames( S.className, className ) }>
        <select className={ S.name }>
            <optgroup label="Select a generator">
                <option selected>{ generatorName }</option>
            </optgroup>
            <option disabled>New...</option>
        </select>
        <span>Context: <SelectContext /></span>
        <span>Data sample: <UploadDataSample /></span>
    </div>
);
