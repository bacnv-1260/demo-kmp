import UIKit
import shared

class UserDetailViewController: UIViewController {
    private let user: GitHubUser
    private var imageTask: Task<Void, Never>?

    private let scrollView = UIScrollView()
    private let containerView = UIView()
    private let avatarImageView = UIImageView()
    private let loginLabel = UILabel()
    private let typeLabel = UILabel()
    private let githubButton = UIButton(configuration: .filled())

    init(user: GitHubUser) {
        self.user = user
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) { fatalError("not implemented") }

    override func viewDidLoad() {
        super.viewDidLoad()
        title = user.login
        navigationItem.largeTitleDisplayMode = .never
        view.backgroundColor = .systemBackground
        setupUI()
        configure()
    }

    deinit { imageTask?.cancel() }

    private func configure() {
        loginLabel.text = user.login
        typeLabel.text = user.type
        githubButton.setTitle("Xem trang GitHub", for: .normal)
        githubButton.addTarget(self, action: #selector(openGitHub), for: .touchUpInside)
        loadAvatar()
    }

    @objc private func openGitHub() {
        guard let url = URL(string: user.htmlUrl) else { return }
        UIApplication.shared.open(url)
    }

    private func loadAvatar() {
        guard let url = URL(string: user.avatarUrl) else { return }
        imageTask = Task {
            guard let (data, _) = try? await URLSession.shared.data(from: url),
                  !Task.isCancelled,
                  let image = UIImage(data: data) else { return }
            await MainActor.run { [weak self] in
                self?.avatarImageView.image = image
            }
        }
    }

    private func setupUI() {
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        containerView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(scrollView)
        scrollView.addSubview(containerView)

        avatarImageView.contentMode = .scaleAspectFill
        avatarImageView.clipsToBounds = true
        avatarImageView.layer.cornerRadius = 60
        avatarImageView.backgroundColor = .secondarySystemFill
        avatarImageView.translatesAutoresizingMaskIntoConstraints = false

        loginLabel.font = .systemFont(ofSize: 24, weight: .bold)
        loginLabel.textAlignment = .center
        loginLabel.translatesAutoresizingMaskIntoConstraints = false

        typeLabel.font = .systemFont(ofSize: 14)
        typeLabel.textColor = .secondaryLabel
        typeLabel.textAlignment = .center
        typeLabel.translatesAutoresizingMaskIntoConstraints = false

        githubButton.translatesAutoresizingMaskIntoConstraints = false

        containerView.addSubview(avatarImageView)
        containerView.addSubview(loginLabel)
        containerView.addSubview(typeLabel)
        containerView.addSubview(githubButton)

        NSLayoutConstraint.activate([
            scrollView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: view.bottomAnchor),

            containerView.topAnchor.constraint(equalTo: scrollView.contentLayoutGuide.topAnchor),
            containerView.leadingAnchor.constraint(equalTo: scrollView.contentLayoutGuide.leadingAnchor),
            containerView.trailingAnchor.constraint(equalTo: scrollView.contentLayoutGuide.trailingAnchor),
            containerView.bottomAnchor.constraint(equalTo: scrollView.contentLayoutGuide.bottomAnchor),
            containerView.widthAnchor.constraint(equalTo: scrollView.frameLayoutGuide.widthAnchor),

            avatarImageView.topAnchor.constraint(equalTo: containerView.topAnchor, constant: 32),
            avatarImageView.centerXAnchor.constraint(equalTo: containerView.centerXAnchor),
            avatarImageView.widthAnchor.constraint(equalToConstant: 120),
            avatarImageView.heightAnchor.constraint(equalToConstant: 120),

            loginLabel.topAnchor.constraint(equalTo: avatarImageView.bottomAnchor, constant: 20),
            loginLabel.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 24),
            loginLabel.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -24),

            typeLabel.topAnchor.constraint(equalTo: loginLabel.bottomAnchor, constant: 8),
            typeLabel.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 24),
            typeLabel.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -24),

            githubButton.topAnchor.constraint(equalTo: typeLabel.bottomAnchor, constant: 24),
            githubButton.centerXAnchor.constraint(equalTo: containerView.centerXAnchor),
            githubButton.bottomAnchor.constraint(equalTo: containerView.bottomAnchor, constant: -32),
        ])
    }
}
