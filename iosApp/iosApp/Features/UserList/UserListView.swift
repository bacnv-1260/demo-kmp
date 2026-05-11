import UIKit
import shared

class UserListViewController: UIViewController {

    // MARK: - UI Components
    private let tableView = UITableView()
    private let loadingIndicator = UIActivityIndicatorView(style: .large)
    private let errorLabel = UILabel()
    private let retryButton = UIButton(configuration: .borderedProminent())

    // MARK: - State
    private var users: [GitHubUser] = []
    private let presenter = UserListPresenter()
    private var tasks: [Task<Void, Never>] = []

    // MARK: - Lifecycle
    override func viewDidLoad() {
        super.viewDidLoad()
        title = "GitHub Users"
        view.backgroundColor = .systemBackground
        setupTableView()
        setupLoadingView()
        setupErrorView()
        startObserving()
    }

    deinit {
        tasks.forEach { $0.cancel() }
    }

    // MARK: - Observation
    private func startObserving() {
        let vm = presenter.viewModel

        tasks.append(Task { @MainActor [weak self] in
            for await state in vm.state {
                self?.render(state: state)
            }
        })

        tasks.append(Task { @MainActor [weak self] in
            for await effect in vm.effect {
                switch onEnum(of: effect) {
                case .navigateToDetail(let nav):
                    self?.push(user: nav.user)
                }
            }
        })
    }

    // MARK: - Rendering
    private func render(state: ViewModelState<UserDataState>) {
        if state.statusState is StatusState.Loading {
            showLoading()
        } else if let error = state.statusState as? StatusState.Error {
            showError(message: error.message ?? "Đã có lỗi xảy ra")
        } else {
            users = state.dataState?.users ?? []
            showContent()
        }
    }

    private func showLoading() {
        loadingIndicator.startAnimating()
        tableView.isHidden = true
        errorLabel.isHidden = true
        retryButton.isHidden = true
    }

    private func showError(message: String) {
        loadingIndicator.stopAnimating()
        errorLabel.text = message
        errorLabel.isHidden = false
        retryButton.isHidden = false
        tableView.isHidden = true
    }

    private func showContent() {
        loadingIndicator.stopAnimating()
        errorLabel.isHidden = true
        retryButton.isHidden = true
        tableView.isHidden = false
        tableView.reloadData()
    }

    private func push(user: GitHubUser) {
        navigationController?.pushViewController(UserDetailViewController(user: user), animated: true)
    }

    // MARK: - Actions
    @objc private func retryTapped() {
        presenter.loadUsers()
    }
}

// MARK: - UI Setup
extension UserListViewController {
    private func setupTableView() {
        tableView.register(UserTableViewCell.self, forCellReuseIdentifier: UserTableViewCell.reuseId)
        tableView.dataSource = self
        tableView.delegate = self
        tableView.rowHeight = 64
        tableView.separatorInset = UIEdgeInsets(top: 0, left: 72, bottom: 0, right: 0)
        tableView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(tableView)

        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: view.topAnchor),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
    }

    private func setupLoadingView() {
        loadingIndicator.hidesWhenStopped = true
        loadingIndicator.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(loadingIndicator)

        NSLayoutConstraint.activate([
            loadingIndicator.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            loadingIndicator.centerYAnchor.constraint(equalTo: view.centerYAnchor),
        ])
    }

    private func setupErrorView() {
        errorLabel.textColor = .systemRed
        errorLabel.numberOfLines = 0
        errorLabel.textAlignment = .center
        errorLabel.isHidden = true
        errorLabel.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(errorLabel)

        retryButton.setTitle("Thử lại", for: .normal)
        retryButton.addTarget(self, action: #selector(retryTapped), for: .touchUpInside)
        retryButton.isHidden = true
        retryButton.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(retryButton)

        NSLayoutConstraint.activate([
            errorLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            errorLabel.centerYAnchor.constraint(equalTo: view.centerYAnchor, constant: -30),
            errorLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 24),
            errorLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -24),

            retryButton.topAnchor.constraint(equalTo: errorLabel.bottomAnchor, constant: 16),
            retryButton.centerXAnchor.constraint(equalTo: view.centerXAnchor),
        ])
    }
}

// MARK: - UITableViewDataSource
extension UserListViewController: UITableViewDataSource {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        users.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: UserTableViewCell.reuseId, for: indexPath) as! UserTableViewCell
        cell.configure(with: users[indexPath.row])
        return cell
    }
}

// MARK: - UITableViewDelegate
extension UserListViewController: UITableViewDelegate {
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        presenter.selectUser(user: users[indexPath.row])
    }
}
