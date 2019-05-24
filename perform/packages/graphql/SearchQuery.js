import { h }                from 'preact';
import { path }             from 'ramda';

import { GqlQuery }         from './index';
import concatResultsInPath  from './concat-results-in-path';


/*  Example gqlQuery:

    query exampleQuery( $searchQuery: String $offset: Int! ) {
        results: yourQuery( searchQuery: $searchQuery offset: $offset ) {
            items { id value }
            offset
            totalCount
        }
    }

    Example usage:

    <SearchQuery
        gqlQuery={ exampleQuery }
        searchQuery={ userInputString }
    >
        { ({ error, items, loading, offset, onFetchMore, totalCount }) =>
            <div>
                { items && items.map( item => <div key={ item.id }>{ item.value }</div> ) }
                <button onClick={ onFetchMore }>Load more</button>
            </div>
        }
    </SearchQuery>
*/

const PATHS = {
    items:                  [ 'results', 'items' ],
    offset:                 [ 'results', 'offset' ],
    totalCount:             [ 'results', 'totalCount' ],
};


export default ({ children, gqlQuery, offset = 0, searchQuery }) =>
    <GqlQuery
        fetchPolicy="cache-and-network"
        notifyOnNetworkStatusChange
        query={ gqlQuery }
        variables={{ offset, searchQuery }}
    >
        { ({ error, data, loading, fetchMore }) =>
            children[0]({
                error,
                items:              path( PATHS.items, data ),
                loading,
                offset:             path( PATHS.offset, data ),
                onFetchMore: () => fetchMore({
                    variables: {
                        offset:     data.results.items.length,
                    },
                    updateQuery:    concatResultsInPath( PATHS.items ),
                }),
                totalCount:         path( PATHS.totalCount, data ),
            })
        }
    </GqlQuery>;
