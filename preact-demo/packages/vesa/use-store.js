import { h, Component } from 'preact';
import shallowEqual     from 'shallow-equal/objects';


export default storeName => Child =>
    class StoreUser extends Component {

        store =         this.context.S[storeName];
        state =         this.store.storeState;

        onStoreChange = state =>
            this.setState( state );

        constructor( props, context ) {
            super( props, context );

            this.store.viewDispatcher.register( this.onStoreChange );
        }

        componentWillUnmount() {

            this.store.viewDispatcher.unregister( this.onStoreChange );
        }

        shouldComponentUpdate( nextProps, nextState, nextContext ) {

            return (
                !shallowEqual( nextProps, this.props )
                || !shallowEqual( nextState, this.state )
            );
        }

        render( props, state, context ) {
            return h( Child, {
                E:              this.context.E,
                [storeName]:    state,
                ...props,
            });
        }
    };
