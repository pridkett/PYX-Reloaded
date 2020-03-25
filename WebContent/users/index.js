class UsersManager {
    constructor() {
        this.main = $('main');

        this.message = this.main.find('.message');
        this.users = new List(this.main[0], {
            item: 'userTemplate',
            valueNames: ['_nick', '_role', {attr: 'src', name: '_img'}]
        });

        this.refresh = $('#refresh');
        this.refresh.on('click', () => this.loadUsers());

        this._drawer = $('#drawer');
        this.drawer = new mdc.drawer.MDCTemporaryDrawer(this._drawer[0]);
        $('.mdc-toolbar__menu-icon').on('click', () => this.drawer.open = true);

        this._theming = $('._themingDialog');
        this._theming.on('click', () => {
            showThemingDialog();
            this.closeDrawer();
        });

        this._logout = $('._logout');
        this._logout.on('click', () => {
            UsersManager.logout();
            this.closeDrawer();
        });

        this._adminPanel = $('._adminPanel');
        this.profilePicture = this._drawer.find('.details--profile');
        this.profileNickname = this._drawer.find('.details--nick');
        this.profileEmail = this._drawer.find('.details--email');
        this.loadUserInfo();
    }

    loadUserInfo() {
        Requester.request("gme", {}, (data) => {
            /**
             * @param {object} data.a - User account
             * @param {string} data.n - User nickname
             * @param {string} data.a.p - Profile picture URL
             * @param {string} data.a.em - Profile email
             */
            Notifier.debug(data);

            this.profileNickname.text(data.n);
            if (data.a !== undefined) {
                if (data.a.p !== null) this.profilePicture.attr('src', data.a.p);
                this.profileEmail.show();
                this.profileEmail.text(data.a.em);
                if (data.a.ia) this._adminPanel.show();
                else this._adminPanel.hide();
            } else {
                this.profileEmail.hide();
                this._adminPanel.hide();
            }
        }, (error) => {
            Notifier.error("Failed loading user info.", error)
        });
    }

    loadUsers() {
        Requester.request("gn", {}, (data) => {
            /**
             * @param {object[]} data.nl - Names list
             * @param {string} data.nl[].n - Nickname
             * @param {string} data.nl[].ia - Whether the user is an admin
             * @param {string} data.nl[].ha - Whether the user has an account
             * @param {string} data.nl[].p - Profile picture
             */

            this.users.clear();
            const list = data.nl;
            for (let i = 0; i < list.length; i++) {
                const user = list[i];
                this.users.add({
                    "_nick": user.n,
                    "_role": user.ia ? "Admin" : (user.ha ? "Registered" : "Guest"),
                    "_img": user.p === undefined || user.p === null ? "/css/images/no-profile.svg" : user.p
                });
            }

            if (list.length === 0) this.message.show();
            else this.message.hide();
        }, (error) => {
            Notifier.error("Failed loading users!", error);
        });
    }

    closeDrawer() {
        this.drawer.open = false;
    }

    static logout() {
        eventsReceiver.close();
        Requester.always("lo", {}, () => {
            window.location = "/";
        });
    }
}