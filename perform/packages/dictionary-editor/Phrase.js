import classnames           from 'classnames';
import { h, Component }     from 'preact';
import PropTypes            from 'prop-types';

import { composeQueries }   from '../graphql/';
import { Error }            from '../ui-messages/';
import { QA }               from '../tests/constants';
import sortFlagUsage        from '../dictionary/sort-reader-flag-usage';
import {
    updatePhraseDefaultUsage,
    updateReaderFlagUsage,
}   from '../graphql/mutations.graphql';
import UsageTd              from '../usage/UsageTd';

import PhraseText           from './PhraseText';


export default composeQueries({
    updatePhraseDefaultUsage,
    updateReaderFlagUsage,
})( class DictionaryEditorPhrase extends Component {

    static propTypes = {
        className:                  PropTypes.string,
        phrase:                     PropTypes.object.isRequired,
        updatePhraseDefaultUsage:   PropTypes.func.isRequired,
        updateReaderFlagUsage:      PropTypes.func.isRequired,
    };

    onChangeDefaultUsage = defaultUsage => {
        const { id } =      this.props.phrase;

        this.props.updatePhraseDefaultUsage({
            variables: {
                id,
                defaultUsage,
            },
            optimisticResponse: {
                __typename:         'Mutation',
                updatePhraseDefaultUsage: {
                    ...this.props.phrase,
                    defaultUsage,
                },
            },
        });
    };

    onChangeFlagUsage = flagUsage => usage => {
        const { id } =      flagUsage;

        this.props.updateReaderFlagUsage({
            variables: {
                id,
                usage,
            },
            optimisticResponse: {
                __typename:         'Mutation',
                updateReaderFlagUsage: {
                    ...flagUsage,
                    usage,
                },
            },
        });
    };

    render({
        className,
        phrase,
        readerFlags,
    }) {
        return (
            <tr className={ classnames( QA.DICT_ITEM_EDITOR_PHRASE, className ) }>
                <td>
                    <PhraseText phrase={ phrase } />
                </td>
                <UsageTd
                    className={ QA.DICT_ITEM_EDITOR_PHRASE_DEFAULT_USAGE }
                    defaultUsage
                    onChange={ this.onChangeDefaultUsage }
                    usage={ phrase.defaultUsage }
                />
                { sortFlagUsage( readerFlags, phrase.readerFlagUsage )
                    .map( flagUsage =>
                        flagUsage
                            ? <UsageTd
                                className={ QA.DICT_ITEM_EDITOR_PHRASE_RFLAG_USAGE }
                                key={ flagUsage.id }
                                onChange={ this.onChangeFlagUsage( flagUsage ) }
                                usage={ flagUsage.usage }
                            />
                            : <td>
                                <Error
                                    justIcon
                                    message="No usage data found for the flag. Try reloading the app."
                                />
                            </td>
                    )
                }
            </tr>
        );
    }
});
