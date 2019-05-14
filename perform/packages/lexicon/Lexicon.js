import classnames       from 'classnames';
import { h, Component } from 'preact';

import { QA }           from '../tests/constants';
import { useStores }    from '../vesa/';

import S                from './Lexicon.sass';
import ItemTable        from './ItemTable';


export default useStores([
    'lexicon',
])( class Lexicon extends Component {

    onChangeSearch = evt =>
        this.props.E.lexicon.onChangeQuery( evt.target.value );

    render({
        E,
        lexicon: {
            newItem,
            newItemSaved,
            query,
        },
    }) {
        return (
            <div className={ S.className }>
                <div className={ S.top }>
                    <button
                        children="➕ New list"
                        className={ classnames( S.new, QA.LEXICON_NEW_BTN ) }
                        disabled={ newItem && ! newItemSaved }
                        onClick={ E.lexicon.onClickNew }
                    />
                    <input
                        className={ classnames( S.search, QA.LEXICON_SEARCH ) }
                        onInput={ this.onChangeSearch }
                        placeholder="search"
                        type="search"
                        value={ query }
                    />
                </div>
                <ItemTable
                    E={ E }
                    lexicon={ this.props.lexicon }
                />
            </div>
        );
    }
});
